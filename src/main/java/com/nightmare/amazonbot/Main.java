package com.nightmare.amazonbot;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.common.util.Snowflake;

public class Main {

    // Token du bot (à remplacer par une variable d'environnement en production)
    public static final String DISCORD_TOKEN = "TON_TOKEN_ICI"; // 🔐 Remplace par ton token réel

    // ID du salon texte Discord où envoyer les messages
    public static final String CHANNEL_ID = "1362520469257064490"; // 📝 Vérifie bien l'ID du salon

    public static void main(String[] args) {
        // Connexion au client Discord
        GatewayDiscordClient client = DiscordClientBuilder.create(DISCORD_TOKEN)
                .build()
                .login()
                .block();

        if (client == null) {
            System.err.println("❌ Impossible de se connecter à Discord.");
            return;
        }

        System.out.println("✅ Bot connecté !");

        // Affiche tous les salons texte pour debug (optionnel)
        client.getGuilds()
            .flatMap(guild -> guild.getChannels())
            .ofType(TextChannel.class)
            .doOnNext(channel -> System.out.println("Salon : " + channel.getName() + " | ID : " + channel.getId().asString()))
            .subscribe();

        // Envoie un message test dans le salon spécifié
        try {
            Snowflake channelId = Snowflake.of(CHANNEL_ID);
            client.getChannelById(channelId)
                    .ofType(TextChannel.class)
                    .flatMap(channel -> channel.createMessage("👋 Hello depuis le bot Amazon !"))
                    .subscribe();
        } catch (Exception e) {
            System.err.println("❌ Erreur d'envoi de message : " + e.getMessage());
        }

        // Empêche le programme de se fermer instantanément
        client.onDisconnect().block();
    }
}
