package com.commercetools.sunrise.payment.methods;

import com.commercetools.sunrise.payment.model.CreatePaymentData;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.carts.commands.CartUpdateCommand;
import io.sphere.sdk.carts.commands.updateactions.AddPayment;
import io.sphere.sdk.carts.commands.updateactions.RemovePayment;
import io.sphere.sdk.commands.Command;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentDraftBuilder;
import io.sphere.sdk.payments.commands.PaymentCreateCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Created by mgatz on 7/10/16.
 */
public abstract class CreatePaymentMethodBase implements CreatePaymentMethod {

    /**
     * Method to To add a new payment to the Cart
     * @param cpd contains the data for the new payment
     * @return the newly created Payment object
     */
    protected CompletionStage<Payment> addNewPayment(final CreatePaymentData cpd) {
        final Cart cart = cpd.getCart();

        final Command<Payment> createPaymentCommand = PaymentCreateCommand.of(createPaymentDraft(cpd).build());
        return cpd.getSphereClient().execute(createPaymentCommand)
                .thenCompose(p -> {
                    return cpd.getSphereClient().execute(CartUpdateCommand.of(cpd.getCart(),AddPayment.of(p) ))
                            .thenApplyAsync(c -> p);
                });
    }

    /**
     * Creates a payment draft object used to create a new payment object at the CTP.
     * @param cpd the data object
     * @return the draft object
     */
    protected PaymentDraftBuilder createPaymentDraft(CreatePaymentData cpd) {

        PaymentDraftBuilder builder = PaymentDraftBuilder
                .of(cpd.getCart().getTotalPrice());

        if(cpd.getCustomer().isPresent()) {
            builder.customer(cpd.getCustomer().get());
        }

        return builder
                .paymentMethodInfo(cpd.getPaymentMethodinInfo());
    }

    /**
     * @param cart {@link Cart} value to read
     * @return Two characters locale name (ISO 639) or <b>null</b> if the {@code cart}, {@code cart.getLocale()} or
     * {@code cart.getLocale().getLanguage()} not exists.
     */
    protected static String getLanguageFromCart(Cart cart) {
        return cart != null && cart.getLocale() != null ? cart.getLocale().getLanguage() : null;
    }
}
