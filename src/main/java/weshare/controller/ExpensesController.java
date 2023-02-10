package weshare.controller;

import io.javalin.http.Handler;
import org.javamoney.moneta.Money;
import org.javamoney.moneta.function.MonetaryFunctions;
import org.jetbrains.annotations.NotNull;
import weshare.model.*;
import weshare.persistence.ExpenseDAO;
import weshare.persistence.PersonDAO;
import weshare.server.Routes;
import weshare.server.ServiceRegistry;
import weshare.server.WeShareServer;

import javax.money.MonetaryAmount;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static weshare.model.DateHelper.TODAY;
import static weshare.model.MoneyHelper.ZERO_RANDS;
import static weshare.model.MoneyHelper.amountOf;

public class ExpensesController {

    public static final Handler view = context -> {
        ExpenseDAO expensesDAO = ServiceRegistry.lookup(ExpenseDAO.class);
        Person personLoggedIn = WeShareServer.getPersonLoggedIn(context);
        Collection<Expense> expenses1 = new ArrayList<>();
        Collection<Expense> expenses = expensesDAO.findExpensesForPerson(personLoggedIn);
        int total = 0;
        for (Expense exp:expenses) {
            if(!(exp.isFullyPaidByOthers())){
                total = total + exp.amountLessPaymentsReceived().getNumber().intValueExact();
                expenses1.add(exp);
            }
        }
        MonetaryAmount tots = Money.of(total, "ZAR");
        Map<String, Object> viewModel = Map.of("expenses", expenses1,"total",tots);
        context.render("expenses.html", viewModel);
    };

    //my changes
    public static final Handler new_expense = context -> {
        context.render("new_expense.html");
//        context.redirect(Routes.EXPENSES);

    };
    public static final Handler payment_requests_received = context -> {
        ExpenseDAO expensesDAO = ServiceRegistry.lookup(ExpenseDAO.class);
        Person personLoggedIn = WeShareServer.getPersonLoggedIn(context);

        Collection<PaymentRequest> paymentRequests = expensesDAO.findPaymentRequestsReceived(personLoggedIn);
        int total = 0;
        for (PaymentRequest exp:paymentRequests) {
            total = total + exp.getAmountToPay().getNumber().intValueExact();
        }
        MonetaryAmount tots = Money.of(total, "ZAR");

        Map<String, Object> viewModel = Map.of("payment_received", paymentRequests,"total",tots);
        context.render("paymentrequests_received.html",viewModel);

    };
    public static final Handler payment_requests_sent = context -> {
        ExpenseDAO expensesDAO = ServiceRegistry.lookup(ExpenseDAO.class);
        Person personLoggedIn = WeShareServer.getPersonLoggedIn(context);

        Collection<PaymentRequest> paymentRequests = expensesDAO.findPaymentRequestsSent(personLoggedIn);
        int total = 0;
        for (PaymentRequest exp:paymentRequests) {
            total = total + exp.getAmountToPay().getNumber().intValueExact();
        }
        MonetaryAmount tots = Money.of(total, "ZAR");
        Map<String, Object> viewModel = Map.of("payment_requests_sent", paymentRequests,"total",tots);
        context.render("paymentrequests_sent.html",viewModel);

    };

    public static final Handler payment_requests = context -> {
        ExpenseDAO expensesDAO = ServiceRegistry.lookup(ExpenseDAO.class);
        String[] id = context.queryString().split("=");
        Optional<Expense> exp = expensesDAO.get(UUID.fromString(id[1]));
        Collection<PaymentRequest> paymentRequests = exp.get().listOfPaymentRequests();
        Map<String, Object> viewModel = Map.of("expense", exp.get(),"expense_requests",paymentRequests,"totalAmountAvailableForPaymentRequests",
                exp.get().totalAmountAvailableForPaymentRequests(),"totalAmountOfPaymentsRequested",exp.get().totalAmountOfPaymentsRequested());
        for (PaymentRequest request : paymentRequests) {
            System.out.println(request.getId());

        }

        context.render("payment_request.html",viewModel);

    };

    public static final Handler add_payment_requests = context -> {
        ExpenseDAO expensesDAO = ServiceRegistry.lookup(ExpenseDAO.class);
        String id = context.formParamAsClass("id",String.class).get();
        Optional<Expense> exp = expensesDAO.get(UUID.fromString(id));
        Expense expense = exp.get();
        String email = context.formParamAsClass("email", String.class)
                .check(Objects::nonNull, "email is required")
                .get();
        int amount = Integer.parseInt(context.formParamAsClass("amount", String.class)
                .check(Objects::nonNull, "Amount is required")
                .get());
        LocalDate date = LocalDate.parse(context.formParamAsClass("due_date", String.class)
                .check(Objects::nonNull, "Date is required")
                .get());
        expense.requestPayment(new Person(email),MoneyHelper.amountOf(amount),date);
        Collection<PaymentRequest> paymentRequests = exp.get().listOfPaymentRequests();
        Map<String, Object> viewModel = Map.of("expense", expense,"expense_requests",paymentRequests,"totalAmountAvailableForPaymentRequests",
                exp.get().totalAmountAvailableForPaymentRequests(),"totalAmountOfPaymentsRequested",expense.totalAmountOfPaymentsRequested());
        context.render("payment_request.html",viewModel);

    };

    public static final Handler addedExpense = context -> {
        ExpenseDAO expensesDAO = ServiceRegistry.lookup(ExpenseDAO.class);
        Person personLoggedIn = WeShareServer.getPersonLoggedIn(context);
        String desc = context.formParamAsClass("description", String.class)
                .check(Objects::nonNull, "Description is required")
                .get();
        int amount = Integer.parseInt(context.formParamAsClass("amount", String.class)
                .check(Objects::nonNull, "Amount is required")
                .get());
        LocalDate date = LocalDate.parse(context.formParamAsClass("date", String.class)
                .check(Objects::nonNull, "Date is required")
                .get());
        Expense expense = new Expense(personLoggedIn, desc, MoneyHelper.amountOf(amount), date);
        expensesDAO.save(expense);
        context.redirect(Routes.EXPENSES);
    };
    public static final Handler paidExpense = context -> {
        ExpenseDAO expensesDAO = ServiceRegistry.lookup(ExpenseDAO.class);
        Person personLoggedIn = WeShareServer.getPersonLoggedIn(context);
        String id = context.formParamAsClass("id",String.class)
                .get();
        System.out.println(id);
        Collection<PaymentRequest> paymentRequests = expensesDAO.findPaymentRequestsReceived(personLoggedIn);
        for (PaymentRequest request : paymentRequests) {
            if (request.getId().toString().equals(id)){
                request.pay(request.getPersonWhoShouldPayBack(),TODAY.minusDays(1));
                expensesDAO.save(new Expense(request.getPersonWhoShouldPayBack(),request.getDescription(), request.getAmountToPay(), request.getDueDate().minusDays(1)));
            }

        }
        context.redirect(Routes.PAYMENTS_RECEIVED);


    };
}
