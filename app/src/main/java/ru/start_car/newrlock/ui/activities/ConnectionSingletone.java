package ru.start_car.newrlock.ui.activities;

import ru.start_car.newrlock.common.client.ClientConnectionService;

/**
 * Created by beerko on 13.06.16.
 */
public class ConnectionSingletone extends ClientConnectionService{
    private static ConnectionSingletone ourInstance = new ConnectionSingletone();
    public static ConnectionSingletone getInstance() {
        return ourInstance;
    }

    private ConnectionSingletone() {
    }
}
