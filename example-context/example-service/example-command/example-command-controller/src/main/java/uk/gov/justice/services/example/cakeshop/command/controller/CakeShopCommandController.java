package uk.gov.justice.services.example.cakeshop.command.controller;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;

import org.slf4j.Logger;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_CONTROLLER)
public class CakeShopCommandController {
    private static final Logger LOGGER = getLogger(CakeShopCommandController.class);

    @Inject
    Sender sender;

    @Handles("cakeshop.add-recipe")
    public void addRecipe(final JsonEnvelope command) {
        sender.send(command);
    }

    @Handles("cakeshop.remove-recipe")
    public void removeRecipe(final JsonEnvelope command) {
        LOGGER.info("=============> Inside remove-recipe Command Handler");

        sender.send(command);
    }

    @Handles("cakeshop.make-cake")
    public void makeCake(final JsonEnvelope command) {
        sender.send(command);
    }

    @Handles("cakeshop.order-cake")
    public void orderCake(final JsonEnvelope command) {
        sender.send(command);
    }
}
