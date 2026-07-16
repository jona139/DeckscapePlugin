package com.deckscape.runelite.ui;

import com.deckscape.runelite.model.DeckscapeCard;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.deckscape.runelite.model.PackType;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import net.runelite.client.util.ImageUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class DeckscapeImages
{
    private static final String WIKI_HOST = "oldschool.runescape.wiki";
    private static final String WIKI_FILE_PATH = "https://oldschool.runescape.wiki/w/Special:FilePath/";
    private static final String USER_AGENT = "Deckscape RuneLite Plugin/0.2.0 (https://github.com/jona139/DeckscapePlugin)";
    private static final int MAX_DOWNLOAD_BYTES = 8 * 1024 * 1024;
    private static final long MAX_IMAGE_PIXELS = 16_000_000L;
    private static final long FAILURE_RETRY_MS = 5 * 60_000L;
    private static final Map<String, BufferedImage> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, BufferedImage> REMOTE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Call> REMOTE_CALLS = new ConcurrentHashMap<>();
    private static final Set<String> REMOTE_PENDING = ConcurrentHashMap.newKeySet();
    private static final Map<String, Long> REMOTE_FAILED = new ConcurrentHashMap<>();
    private static final Map<String, String> LOCAL_FALLBACK_ID = localFallbackIds();
    private static volatile OkHttpClient httpClient;
    private static volatile BooleanSupplier remoteEnabled = () -> false;
    private static volatile Runnable repaintCallback = () -> { };

    private DeckscapeImages() {}

    public static void configureRemoteArt(OkHttpClient client, BooleanSupplier enabled, Runnable repaint)
    {
        httpClient = client;
        remoteEnabled = enabled == null ? () -> false : enabled;
        repaintCallback = repaint == null ? () -> { } : repaint;
    }

    public static void cancelRemoteArtRequests()
    {
        for (Call call : REMOTE_CALLS.values()) call.cancel();
        REMOTE_CALLS.clear();
        REMOTE_PENDING.clear();
    }

    public static void clearRemoteArtConfiguration()
    {
        remoteEnabled = () -> false;
        httpClient = null;
        cancelRemoteArtRequests();
        repaintCallback = () -> { };
    }

    public static BufferedImage load(String resource)
    {
        if (resource == null || resource.isEmpty()) return null;
        return CACHE.computeIfAbsent(resource, key -> ImageUtil.loadImageResource(DeckscapeImages.class, key));
    }

    /** Uses bundled art immediately and schedules a consent-gated HTTPS fallback when it is absent. */
    public static BufferedImage loadCardArt(DeckscapeCard card)
    {
        // Mirror the website's cardArtSources order: current id, explicit legacy
        // resource, known local substitute, then the remote catalog URL.
        BufferedImage bundled = loadOptional(cardResource(card.getId(), ".png"));
        if (bundled == null) bundled = loadOptional(cardResource(card.getId(), ".gif"));
        if (bundled == null) bundled = loadOptional(card.getArtResource());
        String fallbackId = LOCAL_FALLBACK_ID.get(card.getId());
        if (bundled == null && fallbackId != null) bundled = loadOptional(cardResource(fallbackId, ".png"));
        if (bundled != null) return bundled;

        String key = card.getId() + "|" + String.valueOf(card.getArtUrl());
        BufferedImage cached = REMOTE_CACHE.get(key);
        Long failedAt = REMOTE_FAILED.get(key);
        if (failedAt != null && System.currentTimeMillis() - failedAt < FAILURE_RETRY_MS) return null;
        if (failedAt != null) REMOTE_FAILED.remove(key, failedAt);
        if (cached != null || !remoteEnabled.getAsBoolean() || httpClient == null) return cached;

        List<String> candidates = remoteCandidates(card.getName(), card.getArtUrl());
        if (candidates.isEmpty())
        {
            REMOTE_FAILED.put(key, System.currentTimeMillis());
            return null;
        }
        if (REMOTE_PENDING.add(key)) fetchCandidate(key, candidates, 0);
        return null;
    }

    private static BufferedImage loadOptional(String resource)
    {
        if (resource == null || resource.isEmpty() || DeckscapeImages.class.getResource(resource) == null) return null;
        return load(resource);
    }

    private static String cardResource(String id, String extension)
    {
        return "/com/deckscape/runelite/cards/" + id + extension;
    }

    private static Map<String, String> localFallbackIds()
    {
        Map<String, String> ids = new HashMap<>();
        ids.put("abyssal_antibody", "abyssal_walker");
        ids.put("blue_party_hat", "santa_hat");
        ids.put("cave_goblin_child", "cave_goblin_ranger");
        ids.put("dizanas_quiver", "dizana");
        ids.put("dragon_knife", "dragon_dart");
        ids.put("elemental_blast", "elemental_strike");
        ids.put("elemental_bolt", "elemental_strike");
        ids.put("elemental_surge", "elemental_strike");
        ids.put("elemental_wave", "elemental_strike");
        ids.put("giant_wasp", "giant_spider");
        ids.put("gilded_axe", "magic_axe");
        ids.put("ham_joint", "ham_member");
        ids.put("magic_ball", "elemental_strike");
        ids.put("the_crowd_demands_more", "colosseum_guard");
        ids.put("tonalztics_of_ralos", "dizana");
        ids.put("tree_spirit", "spirit_tree");
        return ids;
    }

    static List<String> remoteCandidates(String cardName, String artUrl)
    {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        addArtUrlCandidates(candidates, artUrl);
        addWikiFile(candidates, fileName(cardName, false));
        addWikiFile(candidates, fileName(cardName, true));
        return new ArrayList<>(candidates);
    }

    private static void addArtUrlCandidates(Set<String> candidates, String value)
    {
        if (value == null || value.isEmpty()) return;
        try
        {
            URI uri = URI.create(value.replace(" ", "%20"));
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !WIKI_HOST.equalsIgnoreCase(uri.getHost())) return;
            String path = uri.getPath() == null ? "" : uri.getPath();
            String fragment = uri.getFragment() == null ? "" : URLDecoder.decode(uri.getFragment(), StandardCharsets.UTF_8.name());
            int media = fragment.indexOf("/media/File:");
            if (media >= 0)
            {
                addWikiFile(candidates, fragment.substring(media + "/media/File:".length()));
                return;
            }
            if (path.contains("/Special:FilePath/") || path.startsWith("/images/") || isImagePath(path))
            {
                candidates.add(withoutFragment(value));
                return;
            }
            if (path.startsWith("/w/"))
            {
                String title = URLDecoder.decode(path.substring(3), StandardCharsets.UTF_8.name());
                if (!title.isEmpty()) addWikiFile(candidates, title.endsWith(".png") ? title : title + ".png");
            }
        }
        catch (Exception ignored)
        {
            // Invalid or unsupported URLs deliberately fall through to the name-based wiki fallback.
        }
    }

    private static void addWikiFile(Set<String> candidates, String file)
    {
        if (file == null || file.isEmpty()) return;
        try
        {
            String encoded = URLEncoder.encode(file, StandardCharsets.UTF_8.name()).replace("+", "%20");
            candidates.add(WIKI_FILE_PATH + encoded);
        }
        catch (Exception ignored) { }
    }

    private static String fileName(String cardName, boolean detail)
    {
        if (cardName == null || cardName.isEmpty()) return null;
        return cardName.trim().replace(' ', '_') + (detail ? "_detail.png" : ".png");
    }

    private static boolean isImagePath(String path)
    {
        String lower = path.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif");
    }

    private static String withoutFragment(String value)
    {
        int hash = value.indexOf('#');
        return hash < 0 ? value : value.substring(0, hash);
    }

    private static void fetchCandidate(String key, List<String> candidates, int index)
    {
        if (index >= candidates.size())
        {
            finishFailed(key);
            return;
        }
        if (httpClient == null || !remoteEnabled.getAsBoolean())
        {
            finishCancelled(key);
            return;
        }

        Request request;
        try
        {
            request = new Request.Builder().url(candidates.get(index)).header("User-Agent", USER_AGENT).get().build();
        }
        catch (IllegalArgumentException error)
        {
            fetchCandidate(key, candidates, index + 1);
            return;
        }

        Call call = httpClient.newCall(request);
        REMOTE_CALLS.put(key, call);
        call.enqueue(new Callback()
        {
            @Override
            public void onFailure(Call ignored, IOException error)
            {
                if (remoteEnabled.getAsBoolean()) fetchCandidate(key, candidates, index + 1);
                else finishCancelled(key);
            }

            @Override
            public void onResponse(Call ignored, Response response) throws IOException
            {
                try (Response current = response)
                {
                    if (!remoteEnabled.getAsBoolean())
                    {
                        finishCancelled(key);
                        return;
                    }
                    String contentType = current.header("Content-Type", "");
                    if (!current.isSuccessful() || current.body() == null || !contentType.toLowerCase().startsWith("image/"))
                    {
                        fetchCandidate(key, candidates, index + 1);
                        return;
                    }
                    byte[] bytes = readBounded(current.body().byteStream());
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                    if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0
                        || (long) image.getWidth() * image.getHeight() > MAX_IMAGE_PIXELS)
                    {
                        fetchCandidate(key, candidates, index + 1);
                        return;
                    }
                    REMOTE_CACHE.put(key, image);
                    REMOTE_PENDING.remove(key);
                    REMOTE_CALLS.remove(key);
                    SwingUtilities.invokeLater(repaintCallback);
                }
                catch (IOException error)
                {
                    fetchCandidate(key, candidates, index + 1);
                }
            }
        });
    }

    private static byte[] readBounded(java.io.InputStream input) throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1)
        {
            total += read;
            if (total > MAX_DOWNLOAD_BYTES) throw new IOException("Remote card art exceeds the size limit");
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static void finishFailed(String key)
    {
        REMOTE_PENDING.remove(key);
        REMOTE_CALLS.remove(key);
        REMOTE_FAILED.put(key, System.currentTimeMillis());
    }

    private static void finishCancelled(String key)
    {
        REMOTE_PENDING.remove(key);
        REMOTE_CALLS.remove(key);
    }

    public static String packResource(PackType type)
    {
        switch (type)
        {
            case COMBAT: return "/com/deckscape/runelite/pack_combat.png";
            case SKILLING: return "/com/deckscape/runelite/pack_skilling.png";
            case FACTION_MISTHALIN: return "/com/deckscape/runelite/pack_misthalin.png";
            default: return "/com/deckscape/runelite/pack_general.png";
        }
    }
}
