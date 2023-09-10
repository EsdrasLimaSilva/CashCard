package edu.me.cashcard;

import static org.assertj.core.api.Assertions.*;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.apache.coyote.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;

//the annotation bellow will make the application available for the tests perform requests
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) //this prevents tests interfering each other
class CashCardApplicationTests {

    //tells spring to inject a test helper to make requests
    //to the locally running app
   @Autowired
    TestRestTemplate restTemplate;

   //testing cash card creation
   @Test
   void shouldCreateANewCashCard(){
      //creating a new cashcard
      CashCard cashCard = new CashCard(null, 250.0, null);
      //making a post request
      ResponseEntity<Void> postResponse = restTemplate
              .withBasicAuth("sarah1", "abc123")
              .postForEntity("/cashcards", cashCard, Void.class);
      //verifying that the code is 201 (created)
      assertThat(postResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

      //getting the location send in the postResponse header
      URI locationOfNewCashCard = postResponse.getHeaders().getLocation();
      //making a GET request with that location
      ResponseEntity<String> getResponse = restTemplate
              .withBasicAuth("sarah1", "abc123")
              .getForEntity(locationOfNewCashCard, String.class);
      //seeing if the cash car was really created
      assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

      // checking if the data is correct
      DocumentContext dc = JsonPath.parse(getResponse.getBody());
      Number id = dc.read("$.id");
      Number amount = dc.read("$.amount");
      String owner = dc.read("$.owner");

      assertThat(id).isNotNull();
      assertThat(amount).isEqualTo(250.0);
   }

   //testing the GET method
   @Test
    void shouldReturnACashCardWhenDataIsSaved(){
       //making the api request and storing the response in the variable
       ResponseEntity<String> response = restTemplate
               .withBasicAuth("sarah1", "abc123")
               .getForEntity("/cashcards/99", String.class);

       //checking if the resopnse was 200!
       assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

       //converting the response String into a JSON-aware object with lots of helper methods.
       DocumentContext documentContext = JsonPath.parse(response.getBody());

       //getting the id and verifying that it is not NULL
       Number id = documentContext.read("$.id");
       assertThat(id).isNotNull();

       //checking if the id returned is equal to passed in the request
       assertThat(id).isEqualTo(99);

       //checking the amount
       Number amount = documentContext.read("$.amount");
       assertThat(amount).isEqualTo(123.45);
   }

   @Test
    void shouldNotReturnACashCardWithUnknownId(){
       ResponseEntity<String> response = restTemplate
               .withBasicAuth("sarah1", "abc123")
               .getForEntity("/cashcards/1000", String.class);

       //checking for unknown ids
       assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
       assertThat(response.getBody()).isBlank();
   }

   @Test
   void shouldReturnAListOfCashCards(){
      ResponseEntity<String> response = restTemplate
              .withBasicAuth("sarah1", "abc123")
              .getForEntity("/cashcards", String.class);
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

      DocumentContext dc = JsonPath.parse(response.getBody());
      //seeing if the size of the cashcards is equal to the
      //set in the data.sql test resource
      int cashCardCount = dc.read("$.length()");
      assertThat(cashCardCount).isEqualTo(3);

      //seeing if the data is the same as
      //declared int the "data.sql" file
      JSONArray ids = dc.read("$..id");
      assertThat(ids).containsExactlyInAnyOrder(99, 100, 101);

      JSONArray amounts = dc.read("$..amount");
      assertThat(amounts).containsExactlyInAnyOrder(123.45, 1.0, 150.00);
   }

   @Test
   void shouldReturnAPageOfCashCards() {
      ResponseEntity<String> response = restTemplate
              .withBasicAuth("sarah1", "abc123")
              .getForEntity("/cashcards?page=0&size=1", String.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

      DocumentContext documentContext = JsonPath.parse(response.getBody());
      JSONArray page = documentContext.read("$[*]");
      assertThat(page.size()).isEqualTo(1);
   }

   @Test
   void shouldReturnASortedPageOfCashCards() {
      ResponseEntity<String> response = restTemplate
              .withBasicAuth("sarah1", "abc123")
              .getForEntity("/cashcards?page=0&size=1&sort=amount,desc", String.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

      DocumentContext documentContext = JsonPath.parse(response.getBody());
      JSONArray read = documentContext.read("$[*]");
      assertThat(read.size()).isEqualTo(1);

      double amount = documentContext.read("$[0].amount");
      assertThat(amount).isEqualTo(150.00);
   }

   @Test
   void shouldReturnASortedPageOfCashCardsWithNoParametersAndUseDefaultValues() {
      ResponseEntity<String> response = restTemplate
              .withBasicAuth("sarah1", "abc123")
              .getForEntity("/cashcards", String.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

      DocumentContext documentContext = JsonPath.parse(response.getBody());
      JSONArray page = documentContext.read("$[*]");
      assertThat(page.size()).isEqualTo(3);

      JSONArray amounts = documentContext.read("$..amount");
      assertThat(amounts).containsExactly(1.00, 123.45, 150.00);
   }

   @Test
   void shouldNotAuthorizeRequestsWithAllowedUsers(){
      ResponseEntity<String> response = restTemplate
              .withBasicAuth("random", "99999")
              .getForEntity("/cashcards", String.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

      ResponseEntity<String> responseWrongPassowrd = restTemplate
              .withBasicAuth("sarah1", "wrongpassword")
              .getForEntity("/cashcards", String.class);

      assertThat(responseWrongPassowrd.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
   }

   @Test
   void shouldRejectUsersWhoAreNotCardOwners() {
      ResponseEntity<String> response = restTemplate
              .withBasicAuth("hank-owns-no-cards", "qrs456")
              .getForEntity("/cashcards/99", String.class);
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
   }

   @Test
   void shouldNotAllowDifferentOwnersToSeeCards(){
      ResponseEntity<String> response = restTemplate
              .withBasicAuth("sarah1", "abc123")
              .getForEntity("/cashcards/102", String.class); //card with id 102 is not Sarah's card

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
   }

   @Test
   @DirtiesContext
   void shouldUpdateAnExistingCashCard() {
      CashCard cashCardUpdate = new CashCard(null, 19.99, null);
      HttpEntity<CashCard> request = new HttpEntity<>(cashCardUpdate);
      ResponseEntity<Void> response = restTemplate
              .withBasicAuth("sarah1", "abc123")
              .exchange("/cashcards/99", HttpMethod.PUT, request, Void.class);


      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

      ResponseEntity<String> getResponse = restTemplate
              .withBasicAuth("sarah1", "abc123")
              .getForEntity("/cashcards/99", String.class);
      assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

      DocumentContext dc = JsonPath.parse(getResponse.getBody());

      Number id = dc.read("$.id");
      Number amount = dc.read("$.amount");
      assertThat(amount).isEqualTo(19.99);
      assertThat(id).isEqualTo(99);
   }

   @Test
   void shouldNotUpdateACashCardThatDoesNotExist(){
      CashCard newCashCard = new CashCard(null, 123456.00, null);
      HttpEntity<CashCard> request = new HttpEntity<>(newCashCard);

      ResponseEntity<Void> response = restTemplate
              .withBasicAuth("sarah1", "abc123")
              .exchange("/cashcards/13234", HttpMethod.PUT, request, Void.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
   }
   @Test
   @DirtiesContext
   void shouldDeleteAnExistingCashCard() {
      ResponseEntity<Void> response = restTemplate
              .withBasicAuth("sarah1", "abc123")
              .exchange("/cashcards/99", HttpMethod.DELETE, null, Void.class);
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

      ResponseEntity<String> getResponse = restTemplate
              .withBasicAuth("sarah1", "abc123")
              .getForEntity("/cashcards/99", String.class);
      assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
   }

   @Test
   void shouldNotDeleteACardThatDoesNotExist(){
      ResponseEntity<Void> response = restTemplate
              .withBasicAuth("sarah1", "abc123")
              .exchange("/cashcards/999999", HttpMethod.DELETE, null, Void.class);
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
   }

   @Test
   void shouldNotDeleteACardThatDoesNotOwn(){
      ResponseEntity<Void> response = restTemplate
              .withBasicAuth("sarah1", "abc123")
              .exchange("/cashcards/102", HttpMethod.DELETE, null, Void.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

      
      ResponseEntity<String> getResponse = restTemplate
              .withBasicAuth("kumar2", "xyz789")
              .getForEntity("/cashcards/102", String.class);
      assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

   }

}
