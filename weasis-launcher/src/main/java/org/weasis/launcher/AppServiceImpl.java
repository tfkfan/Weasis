package org.weasis.launcher;

import javafx.application.Application;

public class AppServiceImpl implements AppService {

    private final Application weasisApp;

    public AppServiceImpl(Application weasisApp) {
        this.weasisApp = weasisApp;
    }

    @Override
    public Application getWeasisApp() {
        return weasisApp;
    }
}
