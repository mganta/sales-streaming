package com.foo.sales;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class InvoiceFlattener {

    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    static LocalDate epoch = LocalDate.ofEpochDay(0);

    public static void main(String[] args) throws JSONException {
        String filename = args[0];
        String jsonString = readFile(filename);
        System.out.println(jsonString);
        JSONObject jsonObject = new JSONObject(jsonString);
        parseJSON(jsonObject);
    }

    public static String readFile(String filename) {
        String result = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                line = br.readLine();
            }
            result = sb.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static List<JSONObject> parseJSON(JSONObject jsonObject) throws JSONException {
        Iterator<String> keysIterator = jsonObject.keys();
        JSONObject jObj = new JSONObject();
        List<JSONObject> detailArray = new ArrayList<>();
        List<JSONObject> paymentArray = new ArrayList<>();

        while (keysIterator.hasNext()) {
            String key = keysIterator.next();
            Object obj = jsonObject.get(key);
            if (obj instanceof JSONArray) {
                if (key.equalsIgnoreCase("InvoiceDetail")) {
                    detailArray = processArray(((JSONArray) obj));
                } else if (key.equalsIgnoreCase("InvoicePayment")) {
                    paymentArray = processArray(((JSONArray) obj));
                }
            } else {
                jObj.put(key, obj);
            }
        }

        jObj.put("DaysSinceEpoch", daysSinceEpoch((String) jObj.get("InvoiceDate")));

        List<JSONObject> flattenedArray = new ArrayList<>();

        for (int i = 0; i < detailArray.size(); i++) {
            JSONObject outputObj = new JSONObject();
            JSONObject arrayObj = detailArray.get(i);
            mergeJSONObjects(outputObj, jObj);
            if (paymentArray.size() > 0)
                mergeJSONObjects(outputObj, paymentArray.get(0));
            mergeJSONObjects(outputObj, arrayObj);
            flattenedArray.add(outputObj);
            //System.out.println("output fields " + outputObj.toString());
        }
        return flattenedArray;
    }

    public static List<JSONObject> processArray(JSONArray obj) throws JSONException {
        List<JSONObject> detailArray = new ArrayList<>();
        for (int index = 0; index < obj.length(); index++)
            detailArray.add(obj.getJSONObject(index));
        return detailArray;
    }


    public static void mergeJSONObjects(JSONObject json, JSONObject newObj) throws JSONException {
        Iterator<String> keys = newObj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            String stringValue = newObj.get(key).toString();
            if (!stringValue.equalsIgnoreCase("null"))
                json.put(key, newObj.get(key));
        }
    }

    public static long daysSinceEpoch(String invoiceDate) {
        LocalDate date = LocalDate.parse(invoiceDate, formatter);
        return ChronoUnit.DAYS.between(epoch, date);
    }

}
