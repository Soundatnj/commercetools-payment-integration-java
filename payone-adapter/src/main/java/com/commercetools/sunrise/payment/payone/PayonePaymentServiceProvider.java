package com.commercetools.sunrise.payment.payone;

import com.commercetools.sunrise.payment.domain.PaymentServiceProvider;
import com.commercetools.sunrise.payment.model.CreatePaymentData;
import com.commercetools.sunrise.payment.model.PaymentCreationResult;
import com.commercetools.sunrise.payment.payone.methods.PayoneCreditCardCreatePaymentMethodProvider;
import com.commercetools.sunrise.payment.payone.methods.PayonePaypalCreatePaymentMethodProvider;
import com.commercetools.sunrise.payment.payone.methods.PayoneSofortCreatePaymentMethodProvider;
import com.commercetools.sunrise.payment.utils.PaymentPropertiesLoadingHelper;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentMethodInfo;
import io.sphere.sdk.payments.PaymentStatus;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by mgatz on 7/18/16.
 */
public class PayonePaymentServiceProvider implements PaymentServiceProvider {

    private PaymentPropertiesLoadingHelper propertiesLoadingHelper;

    public PayonePaymentServiceProvider() {
        propertiesLoadingHelper = PaymentPropertiesLoadingHelper.createFromResource("methods/payone.properties");
    }

    @Override
    public String getId() {
        return propertiesLoadingHelper.getProperty("methods.interface");
    }

    @Override
    public List<PaymentMethodInfo> getAvailablePaymentMethods() {
        return getAvailablePaymentMethods(null);
    }

    @Override
    public List<PaymentMethodInfo> getAvailablePaymentMethods(@Nullable Function<List<PaymentMethodInfo>, List<PaymentMethodInfo>> filter) {
        List<PaymentMethodInfo> unfiltered = propertiesLoadingHelper.getAvaiableMethodIds().stream().map(id -> propertiesLoadingHelper.getMethodInfo(id)).collect(Collectors.toList());

        return filter != null
                ? filter.apply(unfiltered)
                : unfiltered;
    }

    @Override
    public Function<CreatePaymentData, CompletionStage<PaymentCreationResult>> provideCreatePaymentHandler(final String methodId) throws UnsupportedOperationException {
        switch (methodId) {
            case "CREDIT_CARD": return PayoneCreditCardCreatePaymentMethodProvider.of().create();
            case "WALLET-PAYPAL": return PayonePaypalCreatePaymentMethodProvider.of().create();
            case "BANK_TRANSFER-SOFORTUEBERWEISUNG": return PayoneSofortCreatePaymentMethodProvider.of().create();
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public BiFunction<Payment, Map<String, String>, Payment> provideCreatePaymentTransactionHandler() {
        return null;
    }

    @Override
    public Function<String, PaymentStatus> provideGetPaymentStatusHandler() {
        return null;
    }
}
