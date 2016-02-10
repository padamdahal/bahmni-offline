package org.bahmni.offline.dbServices.dao;

import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import net.sqlcipher.database.SQLiteDatabase;
import org.bahmni.offline.Constants;
import org.bahmni.offline.MainActivity;
import org.bahmni.offline.Util;
import org.bahmni.offline.Utils.TestUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class SearchDbServiceTest extends ActivityInstrumentationTestCase2<MainActivity>{

    public SearchDbServiceTest() throws KeyManagementException, NoSuchAlgorithmException, IOException {
        super(MainActivity.class);
    }

    private DbHelper mDBHelper;



    public void setUp() throws Exception {
        Context context = getInstrumentation().getTargetContext();
        SQLiteDatabase.loadLibs(context);
        mDBHelper = new DbHelper(context, context.getFilesDir() + "/Bahmni.db");
        mDBHelper.createTable(Constants.CREATE_PATIENT_TABLE);
        mDBHelper.createTable(Constants.CREATE_PATIENT_ADDRESS_TABLE);
        mDBHelper.createTable(Constants.CREATE_PATIENT_ATTRIBUTE_TYPE_TABLE);
        mDBHelper.createTable(Constants.CREATE_PATIENT_ATTRIBUTE_TABLE);


        Util util = Mockito.mock(Util.class);
        when(util.getData(any(URL.class))).thenReturn(TestUtils.readFileFromAssets("patientAttributeTypes.json", getInstrumentation().getContext()));

        PatientDbService patientDbService = new PatientDbService(mDBHelper);
        PatientAttributeDbService patientAttributeDbService = new PatientAttributeDbService(mDBHelper, util);
        PatientAddressDbService patientAddressDbService = new PatientAddressDbService(mDBHelper);

        String uuid = "e34992ca-894f-4344-b4b3-54a4aa1e5558";
        String patientJson = TestUtils.readFileFromAssets("patient.json", getInstrumentation().getContext());
        JSONObject patientData = new JSONObject(patientJson);
        patientDbService.insertPatient(patientData);

        JSONObject person = patientData.getJSONObject("patient").getJSONObject("person");
        JSONArray attributes = person.getJSONArray("attributes");
        JSONObject address = person.getJSONObject("preferredAddress");
        patientAddressDbService.insertAddress(address, uuid);

        SQLiteDatabase db = mDBHelper.getWritableDatabase(Constants.KEY);
        ArrayList<JSONObject> attributeTypeMap = TestUtils.getAttributeTypeMap(db);

        patientAttributeDbService.insertAttributeTypes("https://somehost.com/");
        patientAttributeDbService.insertAttributes(uuid, attributes, attributeTypeMap);
    }

    private void executeSearch(final JSONObject params, final JSONArray[] returnValue) throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    returnValue[0] = new SearchDbService(mDBHelper).execute(String.valueOf(params)).get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }



    @Test
    public void testShouldSearchByFirstName() throws Throwable {

        String searchString = "test";

        final JSONObject params = new JSONObject();
        params.put("q", searchString);
        params.put("s", "byIdOrNameOrVillage");
        params.put("startIndex", 0);
        params.put("address_field_name", "address2");

        final JSONArray[] returnValue = new JSONArray[1];

        executeSearch(params, returnValue);

        JSONObject result = returnValue[0].getJSONObject(0);
        assertEquals("test", result.getString("givenName"));
    }

    @Test
    public void testShouldSearchByLastName() throws Throwable {

        String searchString = "integration";

        final JSONObject params = new JSONObject();
        params.put("q", searchString);
        params.put("s", "byIdOrNameOrVillage");
        params.put("startIndex", 0);
        params.put("address_field_name", "address2");

        final JSONArray[] returnValue = new JSONArray[1];

        executeSearch(params, returnValue);

        JSONObject result = returnValue[0].getJSONObject(0);
        assertEquals("integration", result.getString("familyName"));
    }

    @Test
    public void testShouldSearchByAddress() throws Throwable {

        String searchString = "Chattisgarh";

        final JSONObject params = new JSONObject();
        params.put("q", "");
        params.put("s", "byIdOrNameOrVillage");
        params.put("startIndex", 0);
        params.put("address_field_name", "stateProvince");
        params.put("address_field_value", searchString);

        final JSONArray[] returnValue = new JSONArray[1];

        executeSearch(params, returnValue);

        JSONObject result = returnValue[0].getJSONObject(0);
        assertEquals("Chattisgarh", result.getString("addressFieldValue"));
    }


    @Test
    public void testShouldSearchByIdentifier() throws Throwable {

        String searchString = "GAN200076";

        final JSONObject params = new JSONObject();
        params.put("q", searchString);
        params.put("s", "byIdOrNameOrVillage");
        params.put("startIndex", 0);
        params.put("address_field_name", "address2");

        final JSONArray[] returnValue = new JSONArray[1];

        executeSearch(params, returnValue);

        JSONObject result = returnValue[0].getJSONObject(0);
        assertEquals("GAN200076", result.getString("identifier"));
    }

    @Test
    public void testShouldSearchByAttributes() throws Throwable {

        JSONArray searchArray = new JSONArray().put("caste").put("isUrban").put("education").put("landHolding");
        String searchString = "hindu";

        final JSONObject params = new JSONObject();
        params.put("q", "");
        params.put("s", "byIdOrNameOrVillage");
        params.put("startIndex", 0);
        params.put("address_field_name", "address2");
        params.put("address_field_value", "");
        params.put("custom_attribute", searchString);
        params.put("patientAttributes", searchArray);
        final JSONArray[] returnValue = new JSONArray[1];

        executeSearch(params, returnValue);
        JSONObject result = returnValue[0].getJSONObject(0);
        assertEquals(searchString, new JSONObject(result.getString("customAttribute")).getString("caste"));

        params.put("custom_attribute", true);
        executeSearch(params, returnValue);
        result = returnValue[0].getJSONObject(0);
        assertTrue(new JSONObject(result.getString("customAttribute")).getBoolean("isUrban"));

        searchString = "6th to 9th";
        params.put("custom_attribute", searchString);
        executeSearch(params, returnValue);
        result = returnValue[0].getJSONObject(0);
        assertEquals(searchString, new JSONObject(result.getString("customAttribute")).getString("education"));

        params.put("custom_attribute", 23);
        executeSearch(params, returnValue);
        result = returnValue[0].getJSONObject(0);
        assertEquals(23, new JSONObject(result.getString("customAttribute")).getInt("landHolding"));
    }



}