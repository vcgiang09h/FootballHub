package giangvc.cntt.ntu.footballhub.Utils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class AiHelper {
    public static final String GROQ_API_KEY = "";

    public interface OnEventsExtractedListener {
        void onEventsExtracted(JSONArray eventsArray);
        void onError(String message);
    }

    public interface OnSummaryGeneratedListener {
        void onSummaryGenerated(String summary);
        void onError(String message);
    }

    public static void processImageForEvents(Activity activity, Uri imageUri, OnEventsExtractedListener listener) {
        if (GROQ_API_KEY.isEmpty()) {
            listener.onError("Vui lòng nhập GROQ_API_KEY");
            return;
        }
        
        Toast.makeText(activity, "Đang gửi ảnh lên Groq AI phân tích...", Toast.LENGTH_LONG).show();
        
        new Thread(() -> {
            try {
                InputStream is = activity.getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                
                int maxWidth = 1024;
                int maxHeight = 1024;
                float scale = Math.min(((float)maxWidth / bitmap.getWidth()), ((float)maxHeight / bitmap.getHeight()));
                if (scale < 1) {
                    bitmap = Bitmap.createScaledBitmap(bitmap, (int)(bitmap.getWidth() * scale), (int)(bitmap.getHeight() * scale), true);
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] imageBytes = baos.toByteArray();
                String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
                String dataUri = "data:image/jpeg;base64," + base64Image;
                
                String prompt = "Đây là ảnh chụp biên bản trận đấu bóng đá. Bảng được chia làm 2 NỬA: Nửa bên TRÁI là Đội 1, nửa bên PHẢI là Đội 2.\n"
                              + "Mỗi nửa có các cột: Số áo, Họ và tên, GB (Ghi bàn), PLN (Phản lưới nhà), TV (Thẻ vàng), TĐ (Thẻ đỏ).\n"
                              + "Nhiệm vụ: Phân tích toàn bộ 2 nửa, tìm các cầu thủ có đánh dấu ở cột GB, PLN, TV, TĐ (ví dụ: 1(36') hoặc dấu tick) và trả về ĐÚNG 1 MẢNG JSON, tuyệt đối không giải thích thêm, không dùng dấu ```json:\n"
                              + "[\n  {\"type\": \"Bàn thắng\", \"team\": 1, \"jersey\": 28, \"minute\": \"12\"},\n"
                              + "  {\"type\": \"Thẻ vàng\", \"team\": 2, \"jersey\": 15, \"minute\": \"36\"}\n"
                              + "]\n"
                              + "Quy tắc:\n"
                              + "- type: Chọn 1 trong 4 loại: 'Bàn thắng' (GB), 'Phản lưới nhà' (PLN), 'Thẻ vàng' (TV), hoặc 'Thẻ đỏ' (TĐ).\n"
                              + "- team: 1 (nếu cầu thủ ở nửa bảng bên trái) hoặc 2 (nếu cầu thủ ở nửa bảng bên phải).\n"
                              + "- jersey: Số áo của cầu thủ.\n"
                              + "- minute: Lấy phần số phút nằm trong ngoặc đơn (ví dụ 1(36') lấy 36). Nếu nét chữ là C4 thì xuất 64. Nếu không rõ số phút, để rỗng \"\".";

                JSONObject payload = new JSONObject();
                payload.put("model", "meta-llama/llama-4-scout-17b-16e-instruct");
                payload.put("temperature", 0.1);
                
                JSONArray messages = new JSONArray();
                JSONObject message = new JSONObject();
                message.put("role", "user");
                
                JSONArray contentArray = new JSONArray();
                
                JSONObject textObj = new JSONObject();
                textObj.put("type", "text");
                textObj.put("text", prompt);
                contentArray.put(textObj);
                
                JSONObject imageObj = new JSONObject();
                imageObj.put("type", "image_url");
                JSONObject imageUrlObj = new JSONObject();
                imageUrlObj.put("url", dataUri);
                imageObj.put("image_url", imageUrlObj);
                contentArray.put(imageObj);
                
                message.put("content", contentArray);
                messages.put(message);
                payload.put("messages", messages);

                URL url = new URL("https://api.groq.com/openai/v1/chat/completions");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + GROQ_API_KEY);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = payload.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                if (code == 200) {
                    Scanner scanner = new Scanner(conn.getInputStream());
                    StringBuilder response = new StringBuilder();
                    while (scanner.hasNextLine()) response.append(scanner.nextLine());
                    scanner.close();

                    JSONObject resJson = new JSONObject(response.toString());
                    String rawText = resJson.getJSONArray("choices")
                            .getJSONObject(0).getJSONObject("message").getString("content");

                    int start = rawText.indexOf('[');
                    int end = rawText.lastIndexOf(']');
                    if (start >= 0 && end > start) {
                        String jsonString = rawText.substring(start, end + 1);
                        JSONArray eventsArray = new JSONArray(jsonString);
                        activity.runOnUiThread(() -> listener.onEventsExtracted(eventsArray));
                    } else {
                        activity.runOnUiThread(() -> listener.onError("Không tìm thấy dữ liệu hợp lệ trong ảnh."));
                    }
                } else {
                    activity.runOnUiThread(() -> listener.onError("Lỗi API: " + code));
                }
            } catch (Exception e) {
                e.printStackTrace();
                activity.runOnUiThread(() -> listener.onError("Lỗi xử lý ảnh: " + e.getMessage()));
            }
        }).start();
    }

    public static void generateSummary(Activity activity, String prompt, OnSummaryGeneratedListener listener) {
        if (GROQ_API_KEY.isEmpty()) {
            listener.onError("Bạn cần nhập Groq API Key trong code trước");
            return;
        }
        
        Toast.makeText(activity, "Đang nhờ AI viết bài...", Toast.LENGTH_SHORT).show();
        
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("model", "llama3-70b-8192");
                
                JSONArray messages = new JSONArray();
                JSONObject systemMsg = new JSONObject();
                systemMsg.put("role", "system");
                systemMsg.put("content", "Bạn là một bình luận viên bóng đá chuyên nghiệp. Viết bài tóm tắt trận đấu hay, lôi cuốn, format rõ ràng (có icon emoji) và không dùng Markdown (như ** hay #). Cuối bài luôn kèm hashtag: #BongDaSinhVien #KhoaCNTT #NhietHuyet và liên hệ 'Mọi thắc mắc xin liên hệ Page Đoàn - Hội khoa Công Nghệ Thông Tin'. Trả lời bằng tiếng Việt.");
                
                JSONObject userMsg = new JSONObject();
                userMsg.put("role", "user");
                userMsg.put("content", prompt);
                
                messages.put(systemMsg);
                messages.put(userMsg);
                payload.put("messages", messages);
                payload.put("temperature", 0.7);

                URL url = new URL("https://api.groq.com/openai/v1/chat/completions");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + GROQ_API_KEY);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = payload.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                if (code == 200) {
                    Scanner scanner = new Scanner(conn.getInputStream(), "utf-8");
                    StringBuilder response = new StringBuilder();
                    while (scanner.hasNextLine()) response.append(scanner.nextLine());
                    scanner.close();

                    JSONObject resJson = new JSONObject(response.toString());
                    String aiText = resJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                    
                    activity.runOnUiThread(() -> listener.onSummaryGenerated(aiText));
                } else {
                    activity.runOnUiThread(() -> listener.onError("Lỗi máy chủ AI: " + code));
                }
            } catch (Exception e) {
                e.printStackTrace();
                activity.runOnUiThread(() -> listener.onError("Không kết nối được AI: " + e.getMessage()));
            }
        }).start();
    }
}
