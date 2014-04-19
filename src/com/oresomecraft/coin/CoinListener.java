package com.oresomecraft.coin;

import com.oresomecraft.coin.database.MySQL;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class CoinListener implements Listener {

    public CoinListener() {
        Bukkit.getPluginManager().registerEvents(this, OresomeCoin.getInstance());
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(OresomeCoin.getInstance(), new Runnable() {
            public void run() {
                UUID userId = event.getPlayer().getUniqueId();
                if (OresomeCoin.onlineWallets.get(userId.toString()) != null) return;
                try {
                    MySQL mysql = new MySQL(OresomeCoin.getInstance().getLogger(), "[OresomeCoin]", SQLManager.mysql_host,
                            SQLManager.mysql_port, SQLManager.mysql_db, SQLManager.mysql_user, SQLManager.mysql_password);
                    mysql.open();
                    ResultSet resultSet = mysql.query("SELECT * FROM wallets WHERE uuid = '" + userId.toString() + "';");
                    if (resultSet.isBeforeFirst()) {
                        resultSet.next();
                        String databaseId = resultSet.getString("uuid");
                        if (databaseId != null && !databaseId.equals(" ") && !databaseId.equals("")) {
                            OresomeCoin.onlineWallets.put(userId.toString(), new Wallet(userId, resultSet.getInt("balance")));
                        }
                    } else {
                        mysql.query("INSERT INTO wallets ( uuid, balance ) VALUES ( '" + userId.toString() + "', 0 );");
                        Wallet wallet = new Wallet(userId, 0);
                        OresomeCoin.onlineWallets.put(userId.toString(), wallet);
                        OresomeCoin.getInstance().getLogger().info("Successfully created a wallet for " + userId.toString());
                        mysql.close();
                        if (!OresomeCoin.onlineWallets.containsKey(userId.toString())) {
                            OresomeCoin.getInstance().getLogger().info("DATA STORAGE CONTAINS KEY");
                        }
                        if (!OresomeCoin.onlineWallets.containsValue(wallet)) {
                            OresomeCoin.getInstance().getLogger().info("DATA STORAGE DOES NOT CONTAIN VALUE");
                        }
                    }
                } catch (SQLException ex) {
                    OresomeCoin.getInstance().getLogger().warning("An SQL error occured while attempting to get a UUID's wallet!");
                    OresomeCoin.getInstance().getLogger().warning("UUID = " + userId.toString());
                }
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(OresomeCoin.getInstance(), new Runnable() {
            public void run() {
                UUID userId = event.getPlayer().getUniqueId();
                Wallet wallet = OresomeCoin.onlineWallets.get(userId.toString());
                if (wallet != null) {
                    MySQL mysql = new MySQL(OresomeCoin.getInstance().getLogger(), "[OresomeCoin]", SQLManager.mysql_host,
                            SQLManager.mysql_port, SQLManager.mysql_db, SQLManager.mysql_user, SQLManager.mysql_password);
                    mysql.open();
                    mysql.query("UPDATE wallets SET balance = " + wallet.getBalance() + " WHERE uuid= '" + userId.toString() + "';");
                    OresomeCoin.onlineWallets.remove(userId.toString());
                    OresomeCoin.getInstance().getLogger().info("Successfully pushed a Wallet to the database! [" + wallet.getUserId() + "]");
                    mysql.close();
                }
            }
        });
    }

}