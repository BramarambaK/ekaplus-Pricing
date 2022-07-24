package com.eka.ekaPricing.restAssured;

import static io.restassured.RestAssured.given;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import com.eka.ekaPricing.pojo.AuthenicateRequest;
import com.eka.ekaPricing.pojo.HolidayRuleDetails;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;


public class FormulaControllerPricingScenario1And2 {
	HolidayRuleDetails hDetails = new HolidayRuleDetails();
    List<LocalDateTime> dateRange = new ArrayList<>();
    String token = null;
    String tenantId = null;
    String locale = null;
    String encodedString = null;
    String userName = null;
    String password = null;

    //@Before
    public void setUp() throws Exception {
          System.out.println("Setting up!");
          RestAssured.baseURI = "http://localhost";
          RestAssured.port = 8180;
          userName = "admin";
          password = "Bravo";          
          token = authenticateUser(userName, password);          
          tenantId = "boliden";
          locale = "en_US";         
    }

   // @Test
    public void test_APIWithOAuth2Authentication_ShouldBeGivenAccess() {

          given().header("Authorization", token)
          	.header("Content-Type", "application/json")
            .header("X-TenandID", "boliden")
            .header("X-Locale", "en_US")
            .when()
            .post("/api/pricing/formula")
            .then().statusCode(200);
    }
    
   // @Test
    public void checkForFormulaWIthPayload() throws Exception{
    	JSONObject inputJSON = readJSON("Scenario2.txt");
          System.out.println("input : "+inputJSON);
          try {
                 Thread.sleep(1000);
                 
          }
          catch (Exception e) {
                 
          }

          Response response = 
        		  given()
        		  .header("Authorization", token)
        		  .header("Content-Type", "application/json")
        		  .header("X-TenantID", "boliden")
        		  .header("X-Locale", "en_US")
        		  .body(inputJSON.toString())
          .when()
          .post("/api/pricing/formula");
          
          JSONObject outputJSON = new JSONObject(response.getBody().asString());
          JSONObject contractJson = new JSONObject(outputJSON.getJSONObject("contract").toString());
          JSONArray itemArr = new JSONArray(contractJson.getJSONArray("itemDetails").toString());
          JSONObject item = new JSONObject(itemArr.get(0).toString());
          String price = new JSONObject(item.getJSONArray("resultDataSet").get(0).toString()).getString("contractPrice");
          Assert.assertEquals("Passed", "149.24", price);
    }
   
    
    
public JSONObject readJSON(String path) throws Exception{
		String data = "";
		ClassPathResource cpr = new ClassPathResource(path);
		byte[] bdata = FileCopyUtils.copyToByteArray(cpr.getInputStream());
		data = new String(bdata, StandardCharsets.UTF_8);
		JSONObject jObj = new JSONObject(data);
		return jObj;

	}
    
    private String authenticateUser(String username, String password) {
          String accessToken = null;
          AuthenicateRequest req = new AuthenicateRequest();
          req.setUsername("admin");
          req.setPassword("Bravo");

          Response response = given().header("Content-Type", "application/json").body(req)

                        .when().post("/authenticate/userAuthentication");

          JsonPath jsonPath = new JsonPath(response.asString());
          accessToken = jsonPath.getString("auth2AccessToken.access_token");
          return accessToken;
    }


}
