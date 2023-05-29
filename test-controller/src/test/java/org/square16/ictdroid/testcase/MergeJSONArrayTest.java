package org.square16.ictdroid.testcase;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import org.square16.ictdroid.testcase.provider.BaseValueProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MergeJSONArrayTest {

    @Test
    void testMergeJSONArrayStringDiff() {
        JSONArray jsonArr1 = JSON.parseArray("[\"strValue1\"]");
        JSONArray jsonArr2 = JSON.parseArray("[\"strValue1\", \"strValue2\"]");
        BaseValueProvider.mergeCompJSONArrRecur(jsonArr1, jsonArr2);
        assertEquals(JSON.toJSONString(jsonArr1), JSON.toJSONString(jsonArr2));
    }

    @Test
    void testMergeJSONArrayStringSame() {
        JSONArray jsonArr1 = JSON.parseArray("[\"strValue1\"]");
        JSONArray jsonArr2 = JSON.parseArray("[\"strValue1\"]");
        BaseValueProvider.mergeCompJSONArrRecur(jsonArr1, jsonArr2);
        assertEquals(JSON.toJSONString(jsonArr1), JSON.toJSONString(jsonArr2));
    }

    @Test
    void testMergeJSONArrayNumberDiff() {
        JSONArray jsonArr1 = JSON.parseArray("[1,2,3]");
        JSONArray jsonArr2 = JSON.parseArray("[4,5,1]");
        String res = "[1,2,3,4,5]";
        BaseValueProvider.mergeCompJSONArrRecur(jsonArr1, jsonArr2);
        assertEquals(JSON.toJSONString(jsonArr1), res);
    }

    @Test
    void testMergeJSONArrayNumberSame() {
        JSONArray jsonArr1 = JSON.parseArray("[1,2,3]");
        JSONArray jsonArr2 = JSON.parseArray("[1,2,3]");
        BaseValueProvider.mergeCompJSONArrRecur(jsonArr1, jsonArr2);
        assertEquals(JSON.toJSONString(jsonArr1), JSON.toJSONString(jsonArr2));
    }

    @Test
    void testMergeJSONArrayObjectDiff() {
        JSONArray jsonArr1 = JSON.parseArray("[{\"name\":\"obj1\"}]");
        JSONArray jsonArr2 = JSON.parseArray("[{\"name\":\"obj2\"}]");
        BaseValueProvider.mergeCompJSONArrRecur(jsonArr1, jsonArr2);
        assertEquals(jsonArr1.size(), 2);
        assertEquals(jsonArr1.getJSONObject(0).getString("name"), "obj1");
        assertEquals(jsonArr1.getJSONObject(1).getString("name"), "obj2");
    }

    @Test
    void testMergeJSONArrayObjectSame() {
        JSONArray jsonArr1 = JSON.parseArray("[{\"name\":\"obj1\"}]");
        JSONArray jsonArr2 = JSON.parseArray("[{\"name\":\"obj1\"}]");
        BaseValueProvider.mergeCompJSONArrRecur(jsonArr1, jsonArr2);
        assertEquals(jsonArr1.size(), 1);
        assertEquals(jsonArr1.getJSONObject(0).getString("name"), "obj1");
    }

    @Test
    void testMergeJSONArrayObjectValuesDiff() {
        JSONArray jsonArr1 = JSON.parseArray("[{\"name\":\"obj1\", \"values\": [1, 2]}]");
        JSONArray jsonArr2 = JSON.parseArray("[{\"name\":\"obj1\", \"values\": [2, 3]}]");
        BaseValueProvider.mergeCompJSONArrRecur(jsonArr1, jsonArr2);
        assertEquals(jsonArr1.size(), 1);
        assertEquals(jsonArr1.getJSONObject(0).getJSONArray("values").size(), 3);
    }

    @Test
    void testMergeJSONArrayObjectValuesNullBefore() {
        JSONArray jsonArr1 = JSON.parseArray("[{\"name\":\"obj1\"}]");
        JSONArray jsonArr2 = JSON.parseArray("[{\"name\":\"obj1\", \"values\": [1, 2, 3]}]");
        BaseValueProvider.mergeCompJSONArrRecur(jsonArr1, jsonArr2);
        assertEquals(jsonArr1.size(), 1);
        JSONArray values = jsonArr1.getJSONObject(0).getJSONArray("values");
        assertNotNull(values);
        assertEquals(values.size(), 3);
    }

    @Test
    void testMergeJSONArrayObjectBodyDiff() {
        JSONArray jsonArr1 = JSON.parseArray("[{\"name\":\"obj1\", \"body\": [{\"name\": \"obj1_1\"}]}]");
        JSONArray jsonArr2 = JSON.parseArray("[{\"name\":\"obj1\", \"body\": [{\"name\": \"obj1_2\"}]}]");
        BaseValueProvider.mergeCompJSONArrRecur(jsonArr1, jsonArr2);
        assertEquals(jsonArr1.size(), 1);
        JSONArray body = jsonArr1.getJSONObject(0).getJSONArray("body");
        assertNotNull(body);
        assertEquals(body.size(), 2);
        assertEquals(body.getJSONObject(0).getString("name"), "obj1_1");
        assertEquals(body.getJSONObject(1).getString("name"), "obj1_2");
    }

    @Test
    void testMergeJSONArrayObjectBodyNullBefore() {
        JSONArray jsonArr1 = JSON.parseArray("[{\"name\":\"obj1\"}]");
        JSONArray jsonArr2 = JSON.parseArray("[{\"name\":\"obj1\", \"body\": [{\"name\": \"obj1_1\"}]}]");
        BaseValueProvider.mergeCompJSONArrRecur(jsonArr1, jsonArr2);
        assertEquals(jsonArr1.size(), 1);
        JSONArray body = jsonArr1.getJSONObject(0).getJSONArray("body");
        assertNotNull(body);
        assertEquals(body.getJSONObject(0).getString("name"), "obj1_1");
    }

    @Test
    void testMergeJSONArrayObjectBodyValuesDiff() {
        JSONArray jsonArr1 = JSON.parseArray("[{\"name\":\"obj1\", \"body\": [{\"name\":\"obj1_1\", \"values\":[1,2]}]}]");
        JSONArray jsonArr2 = JSON.parseArray("[{\"name\":\"obj1\", \"body\": [{\"name\":\"obj1_1\", \"values\":[2,3]}]}]");
        BaseValueProvider.mergeCompJSONArrRecur(jsonArr1, jsonArr2);
        assertEquals(jsonArr1.size(), 1);
        JSONArray body = jsonArr1.getJSONObject(0).getJSONArray("body");
        assertNotNull(body);
        assertEquals(body.size(), 1);
        JSONArray values = body.getJSONObject(0).getJSONArray("values");
        assertEquals(values.size(), 3);
    }

    @Test
    void testMergeJSONArrayMix() {
        JSONArray jsonArr1 = JSON.parseArray("[{\"name\":\"obj1\", \"body\": [{\"name\":\"obj1_1\", \"values\":[1,2]}]}]");
        JSONArray jsonArr2 = JSON.parseArray("[{\"name\":\"obj1\", \"body\": [{\"name\":\"obj1_1\", \"values\":[2,3]}]}]");
        JSONArray jsonArr3 = JSON.parseArray("[{\"name\":\"obj2\", \"body\": [{\"name\":\"obj1_1\", \"values\":[1,2,3]}]}]");
        BaseValueProvider.mergeCompJSONArrRecur(jsonArr1, jsonArr2);
        BaseValueProvider.mergeCompJSONArrRecur(jsonArr1, jsonArr3);
        assertEquals(jsonArr1.size(), 2);
        assertEquals(jsonArr1.getJSONObject(1).getString("name"), "obj2");
    }

    @Test
    void test1() {
        System.out.println(String.valueOf(Byte.MIN_VALUE));
    }
}