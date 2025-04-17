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

    private static final Map<String, Double> trackedProducts = new HashMap<>();

    public static void main(String[] args) {
        GatewayDiscordClient client = DiscordClientBuilder.create(DISCORD_TOKEN)
                .build()
                .login()
                .block();

        // ✅ Message de démarrage dans Discord
        client.getChannelById(Snowflake.of(CHANNEL_ID))
                .ofType(MessageChannel.class)
                .subscribe(channel -> channel.createMessage("✅ Le bot Amazon est en ligne !").subscribe());

        // 🔍 Produit Amazon à surveiller (ancien prix fictif)
        trackedProducts.put("https://www.amazon.fr/dp/B07Y45YTFL", 130.00);

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                for (Map.Entry<String, Double> entry : trackedProducts.entrySet()) {
                    String url = entry.getKey();
                    double oldPrice = entry.getValue();

                    try {
                        Document doc = Jsoup.connect(url)
                                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
                                .timeout(10000)
                                .get();

                        // 🧪 Affiche un bout du HTML récupéré pour analyse
                        System.out.println("🧪 HTML récupéré (début) :");
                        System.out.println(doc.html().substring(0, Math.min(1000, doc.html().length())));

                        // 🔎 Essaye de localiser le prix dans les balises classiques
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

                            System.out.println("💰 Prix détecté : " + newPrice + "€");

                            if (newPrice < oldPrice * 0.9) {
                                String title = doc.title();
                                String message = "🔥 **Baisse de prix détectée !**\n"
                                        + "**" + title + "**\n"
                                        + "Ancien prix : " + oldPrice + "€\n"
                                        + "Nouveau prix : " + newPrice + "€\n"
                                        + url;

                                client.getChannelById(Snowflake.of(CHANNEL_ID))
                                        .ofType(MessageChannel.class)
                                        .subscribe(channel -> channel.createMessage(message).subscribe());
                            }

                            trackedProducts.put(url, newPrice);
                        } else {
                            System.out.println("❌ Prix non trouvé : " + url);
                        }
                    } catch (Exception e) {
                        System.out.println("⚠️ Erreur sur " + url + " : " + e.getMessage());
                    }
                }
            }
        }, 0, 30000);

        client.onDisconnect().block();
    }
}
