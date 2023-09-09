package edu.me.cashcard;

import static org.assertj.core.api.Assertions.*;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

//the annotation bellow will make the application available for the tests perform requests
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CashCardApplicationTests {

    //tells spring to inject a test helper to make requests
    //to the locally running app
   @Autowired
    TestRestTemplate restTemplate;

   //testing the GET method
   @Test
    void shouldReturnACashCardWhenDataIsSaved(){
       //making the api request and storing the response in the variable
       ResponseEntity<String> response = restTemplate.getForEntity("/cashcards/99", String.class);

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
       ResponseEntity<String> response = restTemplate.getForEntity("/cashcards/1000", String.class);

       //checking for unknown ids
       assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
       assertThat(response.getBody()).isBlank();
   }
}
