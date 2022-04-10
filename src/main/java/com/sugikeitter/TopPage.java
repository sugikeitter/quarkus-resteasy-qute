package com.sugikeitter;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Path("/")
public class TopPage {
    @Location("index")
    Template index;

    @Location("sync")
    Template sync;

    @Location("async")
    Template async;

    private final SqsClient sqsClient = SqsClient.builder().build();

    private final LambdaClient lambdaClient = LambdaClient.builder().build();

    private static final String LIGHT_QUEUE_URL = System.getenv("LIGHT_QUEUE_URL");

    private static final String LIGHT_LAMBDA_FUNCTION_NAME = System.getenv("LIGHT_LAMBDA_FUNCTION_NAME");

    private static final String HEAVY_QUEUE_URL = System.getenv("HEAVY_QUEUE_URL");

    private static final String HEAVY_LAMBDA_FUNCTION_NAME = System.getenv("HEAVY_LAMBDA_FUNCTION_NAME");
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() {
        List<Order> orders = new ArrayList<>();
        orders.add(new Order(LocalDateTime.ofInstant(Instant.now(), ZoneId.of("Asia/Tokyo")).toString(), "done"));
        orders.add(new Order(LocalDateTime.ofInstant(Instant.now(), ZoneId.of("Asia/Tokyo")).toString(), "accepted"));
        return index.data("orders", orders);
    }

    @POST
    @Path("syncLight")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance postSyncLight() {
        String orderId = LocalDateTime.ofInstant(Instant.now(), ZoneId.of("Asia/Tokyo")).toString();
        InvokeRequest req = InvokeRequest.builder()
                .functionName(LIGHT_LAMBDA_FUNCTION_NAME)
                .payload(SdkBytes.fromUtf8String(orderId))
                .build();
        InvokeResponse res = lambdaClient.invoke(req);
        List<Order> orders = new ArrayList<>();
        return sync
                .data("orders", orders)
                .data("orderId", orderId);
    }

    @POST
    @Path("asyncLight")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance postAsyncLight() {
        String orderId = LocalDateTime.ofInstant(Instant.now(), ZoneId.of("Asia/Tokyo")).toString();
        SendMessageRequest req = SendMessageRequest.builder()
                .queueUrl(LIGHT_QUEUE_URL)
                .messageBody(orderId)
                .build();
        SendMessageResponse res = sqsClient.sendMessage(req);

        List<Order> orders = new ArrayList<>();
        return async
                .data("orders", orders)
                .data("orderId", orderId);
    }
}
