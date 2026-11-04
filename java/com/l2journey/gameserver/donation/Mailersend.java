package com.l2journey.gameserver.donation;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;
import com.l2journey.gameserver.donation.purchase.Purchase;
/** Uses the documented MailerSend REST API, without the old SDK and its attachment dependencies. */
public final class Mailersend
{
 private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(Mailersend.class.getName());
 private static final HttpClient CLIENT=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
 public static void sendPurchaseMail(Purchase p)
 {
  if (!p.getPaymentMethod().sendMail() || DonationConfig.DONATION_MAILER_TOKEN.isBlank() || DonationConfig.DONATION_MAILER_ADDRESS.isBlank()) return;
  DonationExecutor.execute(() ->
  {
   try
   {
    final Map<String,Object> mail=new java.util.HashMap<>();
    mail.put("from",Map.of("email",DonationConfig.DONATION_MAILER_ADDRESS,"name",DonationConfig.DONATION_SERVER_NAME));
    mail.put("to",List.of(Map.of("email",p.getPlayerEmail())));
    mail.put("subject","Payment link - " + DonationConfig.DONATION_SERVER_NAME);
    if (DonationConfig.DONATION_MAILER_TEMPLATE.isBlank()) mail.put("text","Your payment link: " + p.getLink());
    else
    {
     mail.put("template_id",DonationConfig.DONATION_MAILER_TEMPLATE);
     mail.put("personalization",List.of(Map.of("email",p.getPlayerEmail(),"data",Map.of("link",p.getLink(),"name",p.getPlayerName()))));
    }
    final HttpRequest request=HttpRequest.newBuilder(URI.create("https://api.mailersend.com/v1/email")).timeout(Duration.ofSeconds(20)).header("Authorization","Bearer " + DonationConfig.DONATION_MAILER_TOKEN).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(mail))).build();
    final var response=CLIENT.send(request,HttpResponse.BodyHandlers.discarding());
    if (response.statusCode()!=202) throw new IllegalStateException("MailerSend HTTP " + response.statusCode());
   }
   catch (Exception e) { if (e instanceof InterruptedException) Thread.currentThread().interrupt(); LOGGER.warning("Mailersend: Could not send donation email for purchase " + p.getId()); }
  });
 }
}
