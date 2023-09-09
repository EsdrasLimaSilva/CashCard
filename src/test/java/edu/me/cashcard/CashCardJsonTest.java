package edu.me.cashcard;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import java.io.IOException;

//The annotation bellow provides extensive JSON parsing, testing and support
@JsonTest
public class CashCardJsonTest {

    //using Dependency Injection to create a JacksonTester (wrapper) for testing json objects
    @Autowired
    private JacksonTester<CashCard> json;

    //testing the json serialization process
    @Test
    public void cashCardSerializationTest() throws IOException {
        //creating an instafe of CashCard record
        CashCard cashCard = new CashCard(99L, 123.45);

        //writing as a json and comparing with the file at src/test/resources/edu/me/cashcard/expected.json
        assertThat(json.write(cashCard)).isStrictlyEqualToJson("expected.json");
        //checking if the json has the id key
        assertThat(json.write(cashCard)).hasJsonPathNumberValue("@.id");
        //checking if the id key is equal to 99
        assertThat(json.write(cashCard)).extractingJsonPathNumberValue("@.id").isEqualTo(99);
        //checking if the json has the "amount" key
        assertThat(json.write(cashCard)).hasJsonPathNumberValue("@.amount");
        //checking if the amount key is equal to 123.45
        assertThat(json.write(cashCard)).extractingJsonPathNumberValue("@.amount").isEqualTo(123.45);
    }

    //testing the json deserialization process
    @Test
    public void cashCardDeserializationTest() throws IOException {
        //creating a mock json
        String expected = """
                {
                    "id":99,
                    "amount":123.45
                }
                """;
        //checking if the parsed json is equal to the object created
        assertThat(json.parse(expected)).isEqualTo(new CashCard(99L, 123.45));
        //checking if the object json parsed "id" field is equal to "99"
        assertThat(json.parseObject(expected).id()).isEqualTo(99);
        //checking if the object json parsed "amount" field is equal to "123.45"
        assertThat(json.parseObject(expected).amount()).isEqualTo(123.45);
    }
}
