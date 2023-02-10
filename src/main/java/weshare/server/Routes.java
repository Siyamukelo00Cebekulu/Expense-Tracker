package weshare.server;

import weshare.controller.*;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

public class Routes {
    public static final String LOGIN_PAGE = "/";
    public static final String LOGIN_ACTION = "/login.action";
    public static final String LOGOUT = "/logout";
    public static final String EXPENSES = "/expenses";
    public static final String EXPENSES_ACTION = "/expense.action";
    public static final String NEW_EXPENSE = "/newexpense";
    public static final String PAYMENTS_RECEIVED = "/paymentrequests_received";
    public static final String PAYMENT_ACTION = "/payment.action";

    public static final String PAYMENTS_SENT = "/paymentrequests_sent";
    public static final String REQUEST_ACTION = "/paymentrequest";



    public static void configure(WeShareServer server) {
        server.routes(() -> {
            post(LOGIN_ACTION,  PersonController.login);
            get(LOGOUT,         PersonController.logout);
            get(EXPENSES,           ExpensesController.view);
            //my change
            get(NEW_EXPENSE,  ExpensesController.new_expense);
            get(PAYMENTS_RECEIVED,  ExpensesController.payment_requests_received);
            get(PAYMENTS_SENT,  ExpensesController.payment_requests_sent);
            post(EXPENSES_ACTION, ExpensesController.addedExpense);
            get(REQUEST_ACTION, ExpensesController.payment_requests);
            post(REQUEST_ACTION, ExpensesController.add_payment_requests);
            post(PAYMENT_ACTION, ExpensesController.paidExpense);


        });

    }
}
