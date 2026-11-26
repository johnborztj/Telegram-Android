package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FavMessagesHelper {

    public static class FavItem {
        public final int messageId;
        public final String preview;
        public final long savedAt;

        public FavItem(int messageId, String preview, long savedAt) {
            this.messageId = messageId;
            this.preview = preview;
            this.savedAt = savedAt;
        }
    }

    private static final String PREFS_NAME = "fav_messages_prefs";

    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String keyFor(int currentAccount, long dialogId) {
        return "favs_" + currentAccount + "_" + dialogId;
    }

    public static List<FavItem> getFavorites(int currentAccount, long dialogId) {
        List<FavItem> result = new ArrayList<>();
        String raw = getPrefs().getString(keyFor(currentAccount, dialogId), "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj != null) {
                    int id = obj.optInt("id", 0);
                    String preview = obj.optString("preview", "Message #" + id);
                    long savedAt = obj.optLong("savedAt", 0L);
                    if (id != 0) {
                        result.add(new FavItem(id, preview, savedAt));
                    }
                }
            }
        } catch (JSONException ignored) {
        }
        return result;
    }

    public static boolean isFavorite(int currentAccount, long dialogId, int messageId) {
        for (FavItem item : getFavorites(currentAccount, dialogId)) {
            if (item.messageId == messageId) {
                return true;
            }
        }
        return false;
    }

    /**
     * Toggles favorite status for a message.
     * Returns true if the message is now favorited, false if removed.
     */
    public static boolean toggleFavorite(int currentAccount, long dialogId, MessageObject msg) {
        if (msg == null) {
            return false;
        }
        int msgId = msg.getId();
        String preview = buildPreview(msg);
        List<FavItem> current = getFavorites(currentAccount, dialogId);
        JSONArray updated = new JSONArray();
        boolean removed = false;

        for (FavItem item : current) {
            if (item.messageId == msgId) {
                removed = true;
                continue;
            }
            updated.put(toJson(item));
        }

        if (!removed) {
            updated.put(toJson(new FavItem(msgId, preview, System.currentTimeMillis())));
        }

        getPrefs().edit().putString(keyFor(currentAccount, dialogId), updated.toString()).apply();
        return !removed;
    }

    private static JSONObject toJson(FavItem item) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", item.messageId);
            obj.put("preview", item.preview);
            obj.put("savedAt", item.savedAt);
        } catch (JSONException ignored) {
        }
        return obj;
    }

    public static String buildPreview(MessageObject msg) {
        if (msg == null) {
            return "Message";
        }
        if (!TextUtils.isEmpty(msg.messageText)) {
            String clean = msg.messageText.toString().replace('\n', ' ').trim();
            return clean.length() > 70 ? clean.substring(0, 70) + "..." : clean;
        }
        if (!TextUtils.isEmpty(msg.caption)) {
            String clean = msg.caption.toString().replace('\n', ' ').trim();
            return clean.length() > 70 ? clean.substring(0, 70) + "..." : clean;
        }
        if (msg.isPhoto()) {
            return "[Photo]";
        }
        if (msg.isVideo()) {
            return "[Video]";
        }
        if (msg.isVoice() || msg.isRoundVideo()) {
            return "[Voice/Video message]";
        }
        if (msg.isSticker()) {
            return "[Sticker]";
        }
        if (msg.getDocument() != null) {
            return "[File]";
        }
        return "Message #" + msg.getId();
    }
}
