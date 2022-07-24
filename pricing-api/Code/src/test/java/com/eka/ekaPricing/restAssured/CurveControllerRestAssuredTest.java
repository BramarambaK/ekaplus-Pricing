package com.eka.ekaPricing.restAssured;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.*;
import  static org.hamcrest.MatcherAssert.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.eka.ekaPricing.pojo.AuthenicateRequest;
import com.eka.ekaPricing.pojo.HolidayRuleDetails;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

public class CurveControllerRestAssuredTest {
	HolidayRuleDetails hDetails = new HolidayRuleDetails();
    List<LocalDateTime> dateRange = new ArrayList<>();
    String token = null;
    String tenantId = null;
	String locale = null;
	String encodedString = null;
	String userName = null;
	String password = null;
	
	
	/* @Before

	public void setUp() throws Exception {

	        System.out.println("Setting up!");	

	        RestAssured.baseURI = "http://localhost";

	        RestAssured.port = 8080;      

	        userName = "admin";

	        password = "Bravo";

	        //RestAssured.authentication = basic("admin", "Bravo");

	        token = authenticateUser(userName,password);

			tenantId="boliden";

			locale="en_US";

			dateRange.add(LocalDateTime.parse("2018-12-15T00:00"));

	        hDetails.setExchangeName("ICE");

	        hDetails.setHolidayRule("Next Business Day");

	        hDetails.setDateRange(dateRange);

	    }

    

    @Test

    public void test_APIWithOAuth2Authentication_ShouldBeGivenAccess() {   

         

        

    	given().    		  

    		header("Authorization", token)	.

    		header("Content-Type", "application/json")	.	    

		    header("X-TenandID", "boliden").

		    header("X-Locale", "en_US").

    		when().post("/curve/applyHolidayRule").

    	then().

    		statusCode(200);

    }  

    @Test

	 public void whenLogResponseIfErrorOccurredForHolidayAPI_thenSuccess() {

		 given() // Put the level back to what it was

		    .header("Content-Type", "application/json")

		    .header("Authorization", token)

		    .header("X-TenandID", "boliden")

		    .header("X-Locale", "en_US").

		    

	     when().post("/curve/applyHolidayRule")

	       .then().log().ifError();

	     when().post("/applyHolidayRule")

	       .then().log().ifStatusCodeIsEqualTo(500);

	     when().post("/applyHolidayRule")

	       .then().log().ifStatusCodeMatches(greaterThan(200));

	 }

    

    

    private String authenticateUser(String username, String password) {

    	String accessToken = null;    	

    	AuthenicateRequest req = new AuthenicateRequest();

    	req.setUsername("admin");req.setPassword("Bravo");

    	    	

        Response response =

                given().

                header("Content-Type", "application/json").

                body(req)     

                    

                .when()

                    .post("/authenticate/userAuthentication")

                    ;       

        

        JsonPath jsonPath = new JsonPath(response.asString());        

        accessToken = jsonPath.getString("auth2AccessToken.access_token");

        return accessToken;

    }

    */
   
  
}
