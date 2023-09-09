package edu.me.cashcard;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//telling spring that this class is a controller
//and handles requests at /cashcards
@RestController
@RequestMapping("/cashcards")
public class CashCardController {

    //listening for requests at /cashcards/someid e.g. /cashcards/123
    @GetMapping("/{requestedId}")
    public ResponseEntity<CashCard> findById(@PathVariable Long requestedId){
        if(requestedId == 99){
            CashCard cashCard = new CashCard(99L, 456.12);
            return ResponseEntity.ok(cashCard);
        }

        return ResponseEntity.notFound().build();
    }
}
