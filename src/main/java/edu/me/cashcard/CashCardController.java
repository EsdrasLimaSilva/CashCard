package edu.me.cashcard;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.security.Principal;

//telling spring that this class is a controller
//and handles requests at /cashcards
@RestController
@RequestMapping("/cashcards")
public class CashCardController {
    private CashCardRepository cashCardRepository;

    public CashCardController(CashCardRepository cashCardRepository){
        this.cashCardRepository = cashCardRepository;
    }

    private CashCard findCashCard(Long requestedId, Principal principal) {
        return cashCardRepository.findByIdAndOwner(requestedId, principal.getName());
    }

    //listening for requests at /cashcards/someid e.g. /cashcards/123
    @GetMapping("/{requestedId}")
    public ResponseEntity<CashCard> findById(@PathVariable Long requestedId, Principal principal){
        CashCard cashCard = findCashCard(requestedId, principal);

        if(cashCard != null){
            return ResponseEntity.ok(cashCard);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<Iterable<CashCard>> findAll(Pageable pageable, Principal principal){
        Page<CashCard> page = cashCardRepository.findByOwner(principal.getName(),
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSortOr(Sort.by(Sort.Direction.ASC, "amount"))
                ));
        return ResponseEntity.ok(page.getContent());
    }

    @PostMapping
    public ResponseEntity<Void> createCashCard(
            @RequestBody CashCard newCashCardRequest, //spring web will deserialize this
            UriComponentsBuilder ucb, //this is injected by the Spring IoC container automatically,
            Principal principal
    ){
        CashCard cashCardWithOwner = new CashCard(null, newCashCardRequest.amount(), principal.getName());

        CashCard savedCashCard = cashCardRepository.save(cashCardWithOwner);
        URI locationOfNewCashCard = ucb
                .path("/cashcards/{id}")
                .buildAndExpand(savedCashCard.id())
                .toUri();

        return ResponseEntity.created(locationOfNewCashCard).build();
    }

    @PutMapping("/{requestedId}")
    private ResponseEntity<Void> putCashCard(
            @PathVariable Long requestedId,
            @RequestBody CashCard cashCardUpdated,
            Principal principal
    ){
        CashCard cashCard = findCashCard(requestedId, principal);

        if(cashCard != null){
            CashCard newCashCard = new CashCard(cashCard.id(), cashCardUpdated.amount(), principal.getName());
            cashCardRepository.save(newCashCard); //notice that both (old and new) has the same id

            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    private ResponseEntity<Void> deleteCashCard(@PathVariable Long id, Principal principal) {
        if (!cashCardRepository.existsByIdAndOwner(id, principal.getName())) {
            return ResponseEntity.notFound().build();
        }

        cashCardRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
