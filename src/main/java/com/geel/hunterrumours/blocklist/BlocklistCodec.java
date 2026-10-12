package com.geel.hunterrumours.blocklist;

import com.geel.hunterrumours.enums.Hunter;
import com.geel.hunterrumours.enums.Rumour;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Versioned JSON codec for saved and shared blocklists. */
public final class BlocklistCodec
{
    public static final String FORMAT = "hunter-rumours-blocklist";
    public static final int MAX_IMPORT_LENGTH = 32_768;
    private static final Set<String> FIELDS = new HashSet<>(Arrays.asList(
            "format", "version", "id", "name", "activeHunter", "blocks"));
    private static final Gson GSON = new Gson();

    public String exportList(CustomBlocklist list)
    {
        return GSON.toJson(toJson(list, false));
    }

    public String serialize(CustomBlocklist list)
    {
        return GSON.toJson(toJson(list, true));
    }

    public CustomBlocklist importList(String json)
    {
        return parse(json, false);
    }

    public CustomBlocklist deserialize(String json)
    {
        return parse(json, true);
    }

    private JsonObject toJson(CustomBlocklist list, boolean includeId)
    {
        JsonObject root = new JsonObject();
        root.addProperty("format", FORMAT);
        root.addProperty("version", CustomBlocklist.CURRENT_VERSION);
        if (includeId)
        {
            root.addProperty("id", list.getId());
        }
        root.addProperty("name", list.getName());
        root.addProperty("activeHunter", list.getActiveHunter().name());
        JsonObject blocks = new JsonObject();
        for (Map.Entry<Hunter, Rumour> entry : list.getBlocks().entrySet())
        {
            blocks.addProperty(entry.getKey().name(), HunterAssignments.canonical(entry.getValue()).name());
        }
        root.add("blocks", blocks);
        return root;
    }

    private CustomBlocklist parse(String json, boolean requireId)
    {
        if (json == null || json.trim().isEmpty())
        {
            throw new IllegalArgumentException("Blocklist JSON is empty");
        }
        if (json.length() > MAX_IMPORT_LENGTH)
        {
            throw new IllegalArgumentException("Blocklist JSON is too large");
        }

        try
        {
            JsonElement parsed = new JsonParser().parse(json);
            if (!parsed.isJsonObject())
            {
                throw new IllegalArgumentException("Blocklist JSON must be an object");
            }
            JsonObject root = parsed.getAsJsonObject();
            for (String field : root.keySet())
            {
                if (!FIELDS.contains(field))
                {
                    throw new IllegalArgumentException("Unknown blocklist field: " + field);
                }
            }
            if (!FORMAT.equals(string(root, "format")))
            {
                throw new IllegalArgumentException("This is not a Hunter Rumours blocklist");
            }
            int version = integer(root, "version");
            String id = requireId ? string(root, "id") : UUID.randomUUID().toString();
            Hunter active = Hunter.valueOf(string(root, "activeHunter"));
            JsonObject blockJson = object(root, "blocks");
            Map<Hunter, Rumour> blocks = new java.util.EnumMap<>(Hunter.class);
            for (Map.Entry<String, JsonElement> entry : blockJson.entrySet())
            {
                if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString())
                {
                    throw new IllegalArgumentException("Block values must be rumour ids");
                }
                blocks.put(Hunter.valueOf(entry.getKey()), Rumour.valueOf(entry.getValue().getAsString()));
            }
            return new CustomBlocklist(version, id, string(root, "name"), active, blocks);
        }
        catch (IllegalArgumentException ex)
        {
            throw ex;
        }
        catch (RuntimeException ex)
        {
            throw new IllegalArgumentException("Invalid blocklist JSON", ex);
        }
    }

    private static String string(JsonObject object, String field)
    {
        JsonElement value = object.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
        {
            throw new IllegalArgumentException("Missing or invalid " + field);
        }
        return value.getAsString();
    }

    private static int integer(JsonObject object, String field)
    {
        JsonElement value = object.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
        {
            throw new IllegalArgumentException("Missing or invalid " + field);
        }
        return value.getAsInt();
    }

    private static JsonObject object(JsonObject object, String field)
    {
        JsonElement value = object.get(field);
        if (value == null || !value.isJsonObject())
        {
            throw new IllegalArgumentException("Missing or invalid " + field);
        }
        return value.getAsJsonObject();
    }
}
