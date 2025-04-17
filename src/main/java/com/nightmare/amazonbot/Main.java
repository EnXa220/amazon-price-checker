package com.nightmare.amazonbot;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.common.util.Snowflake;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;

public class Main {

    private static final String DISCORD_TOKEN = System.getenv("DISCORD_TOKEN");
    private static final String CHANNEL_ID = System.getenv("DISCORD_CHANNEL_ID");

    // ⚠️ Mets ta clé ScraperAPI ici
    private static final String SCRAPER_API_KEY = System.getenv("SCRAPERAPI_KEY");

    private static final Map<String, Double> trackedProducts = new HashMap<>();

    public static void main(String[] args) {
        GatewayDiscordClient client = DiscordClientBuilder.create(DISCORD_TOKEN)
                .build()
                .login()
                .block();

        // ✅ Message de démarrage
        client.getChannelById(Snowflake.of(CHANNEL_ID))
                .ofType(MessageChannel.class)
                .subscribe(channel -> channel.createMessage("✅ Le bot est en ligne via ScraperAPI !").subscribe());

        // 📦 Produit Amazon avec prix fictif
        trackedProducts.put("https://www.amazon.fr/dp/B07Y45YTFL", 130.00);

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                for (Map.Entry<String, Double> entry : trackedProducts.entrySet()) {
                    String productUrl = entry.getKey();
                    double oldPrice = entry.getValue();

                    String scraperUrl = "http://api.scraperapi.com?api_key=" + SCRAPER_API_KEY + "&url=" + productUrl;

                    try {
                        Document doc = Jsoup.connect(scraperUrl)
                                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                                .timeout(10000)
                                .get();

                        Element priceElement = doc.selectFirst("#priceblock_ourprice, #priceblock_dealprice, span.a-offscreen");

                        if (priceElement == null) {
                            for (Element el : doc.select("span.a-offscreen")) {
                                if (el.text().contains("€")) {
                                    priceElement = el;
                                    break;
                                }
                            }
                        }

                        if (priceElement != null) {
                            String priceText = priceElement.text().replace("€", "").replace(",", ".").trim();
                            double newPrice = Double.parseDouble(priceText);

                            if (newPrice < oldPrice * 0.9) {
                                String message = "🔥 **Baisse de prix détectée !**\n"
                                        + "**" + doc.title() + "**\n"
                                        + "Ancien prix : " + oldPrice + "€\n"
                                        + "Nouveau prix : " + newPrice + "€\n"
                                        + productUrl;

                                client.getChannelById(Snowflake.of(CHANNEL_ID))
                                        .ofType(MessageChannel.class)
                                        .subscribe(channel -> channel.createMessage(message).subscribe());
                            }

                            trackedProducts.put(productUrl, newPrice);
                        } else {
                            System.out.println("❌ Prix non trouvé : " + productUrl);
                        }

                    } catch (Exception e) {
                        System.out.println("⚠️ Erreur : " + e.getMessage());
                    }
                }
            }
        }, 0, 30000);

        client.onDisconnect().block();
    }
}
