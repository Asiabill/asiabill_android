package com.asiabill.testapp.one;




import android.app.Application;

import com.asiabill.testapp.common.environment.PaymentEnvironment;
import com.module.common.utils.AsiaBillMobileSdk;


/**
 * 要想使用BaseApplication，必须在组件中实现自己的Application，并且继承BaseApplication；
 */
public class AsiabillApplication extends Application {
    private static AsiabillApplication sInstance;


    public static AsiabillApplication getInstance() {
        return sInstance;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        AsiaBillMobileSdk.init(this, PaymentEnvironment.PRODUCT);
    }
}
