import java.io.*;
import java.util.HashMap;

public class JsonReader {
    public static HashMap<String, String> readJson(InputStream inputStream){
        StringBuilder jsonBuilder = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                jsonBuilder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String json = jsonBuilder.toString();
        json = json.replaceAll("[{}\"\\[\\]]", "");
        json = json.replace("addresses:", "");
        HashMap<String, String> Addresses = new HashMap<>();

        String[] keyValuePairs = json.split(",");
        for (int i = 0; i < keyValuePairs.length; i+=2) {
            String domain = keyValuePairs[i].split(":")[1];
            String ip = keyValuePairs[i+1].split(":")[1];
            Addresses.put(domain.trim(),ip.trim());
        }
        return Addresses;
    }
}
