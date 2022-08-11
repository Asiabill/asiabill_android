**<h1>Asiabill Android SDK</h1>**

> Welcome to Asiabill's Android SDK. This library will help you accept card and alternative payments in your Android app.



**<h2>asiabill Android sdk对接步骤</h2>**
 
> **<h3> 1. 项目build.gradle里面添加配置<h3>**

 ```
 repositories {
    jcenter()
    maven{
        url "https://raw.githubusercontent.com/Asiabill/asiabill_android/main"
     }
  }
```
 
> **<h3>2. app模块build.gradle里面添加viewbinding支持和 asiabill sdk库<h3>**
 
  
 ```
 buildFeatures{
         viewBinding = true
    }
 ```
 
 ```
 dependencies {
    implementation "com.asiabill.payment:android_payment:2.1.1"//具体版本号根据你的需求来确定
 }
 ```
 
> **<h3>3. app模块gradle.properties里面添加支持androidx库配置<h3>**
 
 ```
 android.useAndroidX=true
 android.enableJetifier=true
 ```

> **<h3>4. application实现sdk初始化<h3>**
 
 ```
public class AsiabillApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //SDK initialization and set up the payment environment.
        AsiaBillMobileSdk.init(this,PaymentEnvironment.PRODUCT);
    }
}
 ```
- SDK初始化时需要传入应用上下文、支付环境等相关信息。
 
 
> **<h3>5. Android sdk调用方法介绍<h3>**

> 对接商户准备工作（商户调用sdk准备工作，可参照demo）

   **<h4>(1) 创建sessionToken<h4>**

 商户服务端调用 <a href="https://asiabill.gitbook.io/api-explorer/api-reference/jiao-yi/sessiontoken" target="_blank">/sessionToken</a> 接口创建本次交易的会话即 sessionToken。
 
   > 建议商户通过服务端调用 /sessionToken 接口获取到 sessionToken、商户号（merNo）、网关接入号（gatewayNo）、签名密钥（signkey ）等相关参数，这些参数会用于后续支付流程的处理，商户需要从 server 端传递到Android端供SDK中的API使用，请妥善保存，避免 signkey 等信息暴露在Android客户端。

   **<h4>(2) 收集支付信息<h4>**
     
   > 商户收集sessionToken 等相关支付信息，构建PayInfoBean，通过调用SDK里payTask.pay(PayInfoBean)方法传递支付信息。
     
   **<h4>(3) 展示付款区域<h4>**
 
   商户发起支付后，会进入sdk里的付款界面，在此收集卡信息发起支付请求。
 
   ![no_card_save](https://raw.githubusercontent.com/Asiabill/asiabill_android/master/img/no_card_save.jpg)
 
   如果传递了customerID字段，则付款区域会展示'Save this card for your future use'选项，商家通过<a href="https://asiabill.gitbook.io/api-explorer/api-reference/jiao-yi/customer" target="_blank">  /customers  </a>接口创建customerID。
 
   ![save_card_in_futere](https://raw.githubusercontent.com/Asiabill/asiabill_android/master/img/save_card_in_futere.jpg)
 
   > 顾客唯一标识customerID（非必须）：Asiabill后台会维护一套顾客管理系统支持客户端付款时保存卡功能，方便顾客未来支付。
 
   **<h4>(4) 相关sdk方法<h4>**
 
   | 方法类型 | 示例| 
   | ------ | ------ |
   | 方法原型	             |      PayTask payTask = new PayTask(activity); String result = payTask.pay(payInfoBean);       |
   | 方法功能	             |        提供给商户订单支付功能                                                  |
   | 方法参数	             |        PayInfoBean(对象赋值传入参数,如payInfoBean.setFirstName("CL")等）       |
   | 返回值	               |        PayResult payResult = new PayResult(result) (详情请看(5))      |

 

 **<h4>(5) 处理支付结果<h4>**
 ```
    PayTask payTask = new PayTask(activity);
    String result = payTask.pay(payInfoBean);
    PayResult payResult = new PayResult(result) 
    String code = payResult.getCode();  //支付返回码
    String message = payResult.getMessage();  //支付描述
 ```
> 通过调用上面SDK的支付方法可以返回支付返回码和描述,相关支付返回码状态如下：
  

| payResult.code（返回码） | payment result（订单结果） | 
| ------ | ------ |
| 9900            |    Successful purchase  （支付成功）        |
| 7700            |    ProFailure purchase  （支付失败）        |
| 6600            |    Order pending        （交易待处理）      |
| 5500            |    Order canceled       （支付取消）        |

- 支付过程中，返回结果中redirectUrl为空，则不需要进行3DS验证，将解析后的交易结果返回给SDK端，SDK端跳转用户到交易结果展示页面。

- 支付过程中，返回结果中redirectUrl不为空，则需要进行3DS验证，将解析后的redirectUrl返回给SDK端，SDK端跳转用户到3DS页面进行验证，
3DS验证完成后，AsiaBill将跳转到商户的returnUrl地址（请参阅 <a href="https://asiabill.gitbook.io/api-explorer/webhook/zhi-fu-jie-guo-tiao-zhuan" target="_blank">支付结果跳转</a> ），商户端验证签名并解析数据后，将解析结果（成功、失败）返回给SDK端，SDK端跳转用户到交易结果展示页面。

> **<h3>6. webhook 通知处理<h3>**

在订单完成后，AsiaBill系统会触发webhook，调用商户交易时给定的callbackUrl，来通知商户交易结果状态，详情请参阅  <a href="https://asiabill.gitbook.io/api-explorer/webhook/gai-shu" target="_blank">webhook</a> 

```
仅接收浏览器端的支付结果是存在风险的，商户网站可能因用户网络或者用户关闭网页导致不能获取到支付结果。建议商户接收 AsiaBill 的支付
结果异步通知，可以通过在收集payInfoBean支付信息步骤中设置 callbackUrl 来指定接收地址。
```



**<h2>Asiabill English version of the Android SDK interworking procedure</h2>**

> **<h3> 1. Add configuration in project build.gradle<h3>**

 ```
 repositories {
    jcenter()
    maven{
        url "https://raw.githubusercontent.com/Asiabill/asiabill_android/main"
     }
  }
```
 
> **<h3>2. Add viewbinding and the asiabillsdk library to the app’s main module build.gradle<h3>**
 
  
 ```
 buildFeatures{
         viewBinding = true
    }
 ```
 
 ```
 dependencies {
    implementation "com.asiabill.payment:android_payment:2.1.1"//The specific version number will be determined according to your needs
 }
 ```
 
> **<h3>3. Add support for androidx library configuration in the app's main module gradle.properties<h3>**
 
 ```
 android.useAndroidX=true
 android.enableJetifier=true
 ```
 
> **<h3>4. Application to achieve the SDK initialization<h3>**
 
 ```
public class AsiabillApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //SDK initialization and set up the payment environment.
        AsiaBillMobileSdk.init(this,PaymentEnvironment.PRODUCT);
    }
}
 ```
- The SDK initialization time need incoming application context and the payment environment.
 
 
> **<h3>5. This section describes how to call the Android SDK<h3>**

> Preparation for merchant connection (preparation for merchant to invoke SDK, please refer to Demo)

   **<h4>(1) Create sessionToken<h4>**

 Merchants server calls <a href="https://asiabill.gitbook.io/api-explorer/api-reference/jiao-yi/sessiontoken" target="_blank">/sessionToken</a> Interface to create the sessionToken .
 
   > Suggest merchants through the server call sessionToken interface to get to the sessionToken, merNo, gatewayNo, signkey, and other related parameters, these parameters will be used for the processing of subsequent payment process, businesses need to transfer from the server end to the Android SDK for the API is used, please keep, avoid signkey information such as exposure to the Android client.

   **<h4>(2) Collect payment information<h4>**
     
   > Merchants gather sessionToken related payment information, such as constructing PayInfoBean, by calling the SDK payTask. Pay (PayInfoBean) method is passed the payment information.
     
   **<h4>(3) Show the payment area<h4>**
 
   Businesses launched after payment, will enter the payment interface in the SDK, in pay the network card information by request.
 
   ![no_card_save](https://raw.githubusercontent.com/Asiabill/asiabill_android/master/img/no_card_save.jpg)
 
   If the delivered the customerID field, the area of payment will show 'Save this card for future use' option,Merchants create customerID through <a href="https://asiabill.gitbook.io/api-explorer/api-reference/jiao-yi/customer" target="_blank">  /customers  </a> interface.
 
   ![save_card_in_futere](https://raw.githubusercontent.com/Asiabill/asiabill_android/master/img/save_card_in_futere.jpg)
 
   > Customers a unique identifier CUSTOMERID (not a must) : Asiabill background will maintain a customer management system to support the client payment saved card function, convenience of our customers to pay in the future.
 
   **<h4>(4) Related to the SDK method<h4>**

   | Interface Name | Interface Description| 
   | ------ | ------ |
   | Method Prototype           |  PayTask payTask = new PayTask(activity); String result = payTask.pay(PayInfoBean)     |
   | Method Function            |  Provides merchant order payment function                                 |
   | Method Parameter           |  PayInfoBean(object assignment passes in the parameter payInfoBean.setFirstName(“CL”), etc.)             |
   | Return Value               |  PayResult payResult = new PayResult(result)    （see (5) for details）   |
 

 **<h4>(5) Handle payment results<h4>**
 ```
    PayTask payTask = new PayTask(activity);
    String result = payTask.pay(payInfoBean);
    PayResult payResult = new PayResult(result) 
    String code = payResult.getCode();  //Pay a return code
    String message = payResult.getMessage();  //Payment description
 ```
> By calling the SDK above method can return payment return code and description, the related state of pay return code is as follows:
  
| payResult.code（Return code） | payment result（Order the results） | 
| ------ | ------ |
| 9900            |    Successful purchase  （Pay for success）        |
| 7700            |    ProFailure purchase  （Pay for failure）        |
| 6600            |    Order pending        （Transaction to be processed）      |
| 5500            |    Order canceled       （For cancellation）        |

- Payment process, return the result of redirectUrl is empty, you don't need to verified the 3 ds, will be returned to the SDK end parsed trading results, the SDK end users to the trading results show page jump.

- payment process, return the result of redirectUrl is not empty, you will need to verified the 3 ds, will be parsed redirectUrl returned to the SDK, the SDK side jump users to 3 ds page for authentication, 3 ds verification is completed, AsiaBill will jump to merchants returnUrl address（Please refer to the <a href="https://asiabill.gitbook.io/api-explorer/webhook/zhi-fu-jie-guo-tiao-zhuan" target="_blank">Pay the jump</a> ），Merchants client-side validation after signature and analytical data, analytical result is returned to the SDK (success, failure), the SDK end users to the trading results show page jump.

> **<h3>6. Webhook inform processing<h3>**

AsiaBill, after the completion of the order system will trigger webhook, call the merchants deal given callbackUrl, to inform the merchant trading results, please refer to  <a href="https://asiabill.gitbook.io/api-explorer/webhook/gai-shu" target="_blank">webhook</a> 

```
Only accept the outcome of the browser side payment is risky, merchant sites may caused by the user's network or the user 
closes the page can't get to pay for results.Suggest merchants receive AsiaBill results asynchronous notification of payment,  
can be set by paid by collecting payInfoBean information step callbackUrl to specify the receiving address.
```
