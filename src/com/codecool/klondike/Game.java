package com.codecool.klondike;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;

import javax.swing.*;
import java.util.*;
import java.util.List;

public class Game extends Pane {

    public static final int TOTAL_NUMBER_OF_CARDS = 52;
    public static final int NUMBER_OF_FOUNDATION_PILES = 4;
    private List<Card> deck;
    private List<Card> shuffledDeck = new ArrayList<Card>();
    private Pile stockPile;
    private Pile discardPile;
    private List<Pile> foundationPiles = FXCollections.observableArrayList();
    private List<Pile> tableauPiles = FXCollections.observableArrayList();

    private double dragStartX, dragStartY;
    private List<Card> draggedCards = FXCollections.observableArrayList();

    private static double STOCK_GAP = 1;
    private static double FOUNDATION_GAP = 0;
    private static double TABLEAU_GAP = 30;


    private EventHandler<MouseEvent> onMouseClickedHandler = e -> {
        Card card = (Card) e.getSource();
        int suitOfCard = card.getSuit();
        Pile destPile = foundationPiles.get(suitOfCard - 1);
        if (e.getClickCount() == 1) {
            if (card.getContainingPile().getPileType() == Pile.PileType.STOCK &&
                    card.equals(stockPile.getTopCard())) {
                card.moveToPile(discardPile);
                card.flip();
                card.setMouseTransparent(false);
                System.out.println("Placed " + card + " to the waste.");
            }
        } else if (e.getClickCount() == 2) {
            if (sendCardToFoundation(card)) {
                Pile sourcePile = card.getContainingPile();
                card.moveToPile(destPile);
                if (!sourcePile.isEmpty()) {
                    if (sourcePile.getPileType() == Pile.PileType.TABLEAU && sourcePile.getTopCard().isFaceDown()) {
                        sourcePile.getTopCard().flip();
                    }
                }
            }
            if (isGameWon()) {
                JOptionPane.showMessageDialog(null, "Congratulations, you won !!!");
                System.exit(0);
            }
        }

    };

    private boolean sendCardToFoundation(Card card) {
        int suitOfCard = card.getSuit();
        int rankOfCard = card.getRank();
        Pile destPile = foundationPiles.get(suitOfCard - 1);
        if (rankOfCard == 1) {
            return true;
        } else {
            if (!destPile.isEmpty() && destPile.getTopCard().getRank() == rankOfCard - 1) {
                return true;
            }
        }
        return false;
    }


    private EventHandler<MouseEvent> stockReverseCardsHandler = e -> {
        refillStockFromDiscard();
    };

    private EventHandler<MouseEvent> onMousePressedHandler = e -> {
        dragStartX = e.getSceneX();
        dragStartY = e.getSceneY();
    };

    private EventHandler<MouseEvent> onMouseDraggedHandler = e -> {
        Card card = (Card) e.getSource();
        Pile activePile = card.getContainingPile();
        if (activePile.getPileType() == Pile.PileType.TABLEAU && card.isFaceDown()) {
            return;
        } else if (activePile.getPileType() == Pile.PileType.DISCARD &&
                !card.equals(discardPile.getTopCard())) {
            return;
        } else if (activePile.getPileType() == Pile.PileType.STOCK) {
            return;
        }
        draggedCards.clear();

        double offsetX = e.getSceneX() - dragStartX;
        double offsetY = e.getSceneY() - dragStartY;

        if (!card.equals(activePile.getTopCard())) {
            boolean matchedCard = false;
            for (Card currentCard : activePile.getCards()) {
                if (!matchedCard) {
                    if (currentCard.equals(card)) {
                        matchedCard = true;
                        draggedCards.add(currentCard);
                    }
                } else {
                    draggedCards.add(currentCard);
                }
            }
        } else {
            draggedCards.add(card);
        }
        for (Card currentCard : draggedCards) {
            currentCard.toFront();
            currentCard.setTranslateX(offsetX);
            currentCard.setTranslateY(offsetY);
            currentCard.getDropShadow().setRadius(20);
            currentCard.getDropShadow().setOffsetX(10);
            currentCard.getDropShadow().setOffsetY(10);
        }
    };

    private EventHandler<MouseEvent> onMouseReleasedHandler = e -> {
        if (draggedCards.isEmpty()) {
            return;
        }
        Card card = (Card) e.getSource();
        Pile pile = checkDestPile(card);
        Pile sourcePile = card.getContainingPile();

        if (pile != null) {
            ListIterator<Card> iter = sourcePile.getCards().listIterator();
            Card cc;
            boolean iterCont = true;
            while (iter.hasNext() && iterCont) {
                cc = iter.next();
                if (cc.equals(card)) {
                    cc = iter.previous();
                    if (iter.hasPrevious()) cc = iter.previous();
                    if (cc.isFaceDown()) {
                        cc.flip();
                        iter.next();
                        iterCont = false;
                    } else {
                        iterCont = false;
                    }
                }
            }
            card.moveToPile(pile);
            Card sourceTopCard = sourcePile.getTopCard();
            if (pile.getPileType().equals(Pile.PileType.TABLEAU)) {
                if (sourceTopCard != null &&
                        sourceTopCard.getContainingPile().getPileType().equals(Pile.PileType.TABLEAU)) {
                    if (sourceTopCard.isFaceDown()) {
                        sourceTopCard.flip();
                    }
                }
            } else if (pile.getPileType().equals(Pile.PileType.FOUNDATION)) {
                if (sourceTopCard != null &&
                        sourceTopCard.getContainingPile().getPileType().equals(Pile.PileType.TABLEAU)) {
                    if (sourceTopCard.isFaceDown()) {
                        sourceTopCard.flip();
                    }
                }
            }
            handleValidMove(card, pile);
            if (isGameWon()) {
                JOptionPane.showMessageDialog(null, "Congratulations, you won !!!");
                System.exit(0);
            }
        } else {
            for (Card cCard : draggedCards) {
                cCard.toFront();
                MouseUtil.slideBack(cCard);
            }
            draggedCards.clear();
        }
    };

    private void refillTheStock() {
        ObservableList<Card> temp = discardPile.getCards();
        Collections.reverse(temp);
        for (Card card : temp) {
            stockPile.addCard(card);
            card.flip();
        }
        discardPile.clear();
        stockPile.setLayoutX(95);
        stockPile.setLayoutY(20);
        discardPile.setLayoutX(285);
        discardPile.setLayoutY(20);
    }


    public boolean isGameWon() {
        int sum = 0;
        for (int i = 0; i < NUMBER_OF_FOUNDATION_PILES; i++) {
            sum += foundationPiles.get(i).numOfCards();
        }
        return (sum == TOTAL_NUMBER_OF_CARDS);
    }

    public Game() {
        deck = Card.createNewDeck();
        Collections.shuffle(deck);
        for (int i = 0; i < deck.size(); i++) {
            Card tempCard = new Card(deck.get(i).getSuit(), deck.get(i).getRank(), true);
            shuffledDeck.add(tempCard);
        }
        initPiles();
        dealCards();
        createButtons();
    }

    private void createButtons() {
        Button restartBtn = new Button("Restart");
        restartBtn.defaultButtonProperty();
        restartBtn.setOnMouseClicked((mouseEvent) -> {
            getChildren().clear();
            restartedGame();
        });
        getChildren().add(restartBtn);
    }

    public void restartedGame() {
        tableauPiles.clear();
        foundationPiles.clear();
        deck.clear();
        for (int i = 0; i < shuffledDeck.size(); i++) {
            Card tempCard = new Card(shuffledDeck.get(i).getSuit(), shuffledDeck.get(i).getRank(), true);
            deck.add(tempCard);
        }
        //deck = shuffledDeck;
        initPiles();
        dealCards();
        createButtons();
    }


    public void addMouseEventHandlers(Card card) {
        card.setOnMousePressed(onMousePressedHandler);
        card.setOnMouseDragged(onMouseDraggedHandler);
        card.setOnMouseReleased(onMouseReleasedHandler);
        card.setOnMouseClicked(onMouseClickedHandler);
    }

    public void refillStockFromDiscard() {
        if (stockPile.isEmpty()) {
            refillTheStock();
            System.out.println("Stock refilled from discard pile.");
        }
    }


    public boolean isMoveValid(Card card, Pile destPile) {
        if (destPile.getPileType().equals(Pile.PileType.TABLEAU)) {
            if (destPile.isEmpty()) {
                return card.getRank() == 13;
            } else {
                Card destCard = destPile.getTopCard();
                if (destCard.isCardColorRed() != card.isCardColorRed() &&
                        destCard.getRank() == card.getRank() + 1) {
                    return true;
                }
            }
        } else if (destPile.getPileType().equals(Pile.PileType.FOUNDATION)) {
            if (destPile.getTopCard() == null) {
                if (card.getRank() == 1 && card.getSuit() == destPile.getName()) {
                    return true;
                }
                return false;

            } else {
                Card destCard = destPile.getTopCard();
                if (destCard.getSuit() == card.getSuit() &&
                        destCard.getRank() == card.getRank() - 1) {
                    return true;
                }
            }
        }
        return false;
    }

    private Pile getValidIntersectingPile(Card card, List<Pile> piles) {
        Pile result = null;
        for (Pile pile : piles) {
            if (!pile.equals(card.getContainingPile()) &&
                    isOverPile(card, pile) &&
                    isMoveValid(card, pile))
                result = pile;
        }
        return result;
    }

    private boolean isOverPile(Card card, Pile pile) {
        if (pile.isEmpty())
            return card.getBoundsInParent().intersects(pile.getBoundsInParent());
        else
            return card.getBoundsInParent().intersects(pile.getTopCard().getBoundsInParent());
    }

    private void handleValidMove(Card card, Pile destPile) {
        String msg = null;
        if (destPile.isEmpty()) {
            if (destPile.getPileType().equals(Pile.PileType.FOUNDATION))
                msg = String.format("Placed %s to the foundation.", card);
            if (destPile.getPileType().equals(Pile.PileType.TABLEAU))
                msg = String.format("Placed %s to a new pile.", card);
        } else {
            msg = String.format("Placed %s to %s.", card, destPile.getTopCard());
        }
        System.out.println(msg);
        MouseUtil.slideToDest(draggedCards, destPile);
        draggedCards.clear();
    }


    private void initPiles() {
        stockPile = new Pile(Pile.PileType.STOCK, 101, STOCK_GAP);
        stockPile.setBlurredBackground();
        stockPile.setLayoutX(95);
        stockPile.setLayoutY(20);
        stockPile.setOnMouseClicked(stockReverseCardsHandler);
        getChildren().add(stockPile);

        discardPile = new Pile(Pile.PileType.DISCARD, 100, STOCK_GAP);
        discardPile.setBlurredBackground();
        discardPile.setLayoutX(285);
        discardPile.setLayoutY(20);
        getChildren().add(discardPile);

        for (int i = 1; i < 5; i++) {
            Pile foundationPile = new Pile(Pile.PileType.FOUNDATION, i, FOUNDATION_GAP);
            foundationPile.setBlurredBackground();
            foundationPile.setLayoutX(610 + (i - 1) * 180);
            foundationPile.setLayoutY(20);
            foundationPiles.add(foundationPile);
            getChildren().add(foundationPile);
        }
        for (int i = 0; i < 7; i++) {
            Pile tableauPile = new Pile(Pile.PileType.TABLEAU, i, TABLEAU_GAP);
            tableauPile.setBlurredBackground();
            tableauPile.setLayoutX(95 + i * 180);
            tableauPile.setLayoutY(275);
            tableauPiles.add(tableauPile);
            getChildren().add(tableauPile);
        }
    }

    public void dealCards() {
        Iterator<Card> deckIterator = deck.iterator();
        Card nextCard;
        for (int tableauNumber = 0; tableauNumber < 7; tableauNumber++) {
            Pile actualPile = tableauPiles.get(tableauNumber);
            for (int cardNumber = 0; cardNumber < tableauNumber + 1; cardNumber++) {
                nextCard = deckIterator.next();
                if (cardNumber == tableauNumber) {
                    nextCard.flip();
                }
                actualPile.addCard(nextCard);
                addMouseEventHandlers(nextCard);
                getChildren().add(nextCard);
            }
        }

        deckIterator.forEachRemaining(card -> {
            stockPile.addCard(card);
            addMouseEventHandlers(card);
            getChildren().add(card);
        });
    }

    public void setTableBackground(Image tableBackground) {
        setBackground(new Background(new BackgroundImage(tableBackground,
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
    }


    private Pile checkDestPile(Card card) {
        Pile pile = getValidIntersectingPile(card, foundationPiles);
        if (pile != null) {
            return pile;
        }
        pile = getValidIntersectingPile(card, tableauPiles);
        if (pile != null) {
            return pile;
        }
        return null;
    }


}
