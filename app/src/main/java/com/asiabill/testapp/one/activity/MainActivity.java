package com.asiabill.testapp.one.activity;
/*
                                          * Copyright 2020 Google Inc.
                                          *
                                          * Licensed under the Apache License, Version 2.0 (the "License");
                                          * you may not use this file except in compliance with the License.
                                          * You may obtain a copy of the License at
                                          *
                                          *     http://www.apache.org/licenses/LICENSE-2.0
                                          *
                                          * Unless required by applicable law or agreed to in writing, software
                                          * distributed under the License is distributed on an "AS IS" BASIS,
                                          * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
                                          * See the License for the specific language governing permissions and
                                          * limitations under the License.
                                          */

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.asiabill.testapp.common.environment.PaymentEnvironment;
import com.asiabill.testapp.manager.PayTask;

import com.asiabill.testapp.model.BaseResponse;
import com.asiabill.testapp.model.GoodsDetail;
import com.asiabill.testapp.model.PayInfoBean;
import com.asiabill.testapp.model.PaymentUiData;
import com.asiabill.testapp.model.request.CreateCustomerRequest;
import com.asiabill.testapp.model.request.SessionTokenRequest;
import com.asiabill.testapp.model.response.CreateCustomerResponse;
import com.asiabill.testapp.model.response.SessionTokenResponse;
import com.asiabill.testapp.network.PaymentApiService;
import com.asiabill.testapp.one.R;
import com.asiabill.testapp.one.databinding.ActivityMainBinding;

import com.asiabill.testapp.one.model.PayResult;

import com.module.common.base.BaseActivity;
import com.module.common.utils.AsiaBillRetrofitClient;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import static com.asiabill.testapp.one.activity.PaymentResultActivity.ALIPAY_PAYMENT_AMOUNT;
import static com.asiabill.testapp.one.activity.PaymentResultActivity.ALIPAY_PAYMENT_CURRENCY;
import static com.asiabill.testapp.one.activity.PaymentResultActivity.ALIPAY_PAYMENT_METHOD;
import static com.asiabill.testapp.one.activity.PaymentResultActivity.ALIPAY_PAYMENT_ORDERID;
import static com.asiabill.testapp.one.activity.PaymentResultActivity.ALIPAY_PAYMENT_RESULT;

/**
 * Checkout implementation for the app
 */
public class MainActivity extends BaseActivity {
    // 国际信用卡2.1,商户测试地址
    public final static String BASE_TPO_URL_TEST = "https://sandbox-pay.asiabill.com/";
    public final static String CRAET_SESSION_TOKEN = "services/v3/sessionToken";// 2.1creat sessionToken

    // 国际信用卡2.1,商户正式地址
    public final static String BASE_TPO_URL = "https://safepay.asiabill.com/";
    public final static String CRAET_CUSTOMERS = "services/v3/customers";// 2.1creat customers
    // 支付方式
    public static final String CreditCardStr = "Credit Card";

    private ActivityMainBinding layoutBinding;

    private long clickTime = 0;

    @SuppressLint("HandlerLeak")
    private Handler mHandle = new Handler() {
        public void handleMessage(Message msg) {
            PayResult payResult = new PayResult((String) msg.obj);
            Log.e("=======msg", payResult.toString());
            String code = payResult.getCode();
            Intent intent = new Intent(MainActivity.this, PaymentResultActivity.class);
            intent.putExtra(ALIPAY_PAYMENT_AMOUNT, ORDER_AMOUNT);
            intent.putExtra(ALIPAY_PAYMENT_CURRENCY, ORDER_CURRENCY);
            intent.putExtra(ALIPAY_PAYMENT_METHOD, paymentMethod);
            if (TextUtils.equals(code, "9900")) {
                intent.putExtra(ALIPAY_PAYMENT_RESULT, 1);
                intent.putExtra(ALIPAY_PAYMENT_ORDERID, ORDERNO);
                MainActivity.this.startActivity(intent);
                Toast.makeText(MainActivity.this, R.string.asiabill_buy_item_successs,
                        Toast.LENGTH_SHORT).show();
            } else {
                if (TextUtils.equals(code, "7700")) {
                    intent.putExtra(ALIPAY_PAYMENT_RESULT, 2);
                    intent.putExtra(ALIPAY_PAYMENT_ORDERID, ORDERNO);
                    MainActivity.this.startActivity(intent);
                    Toast.makeText(MainActivity.this, R.string.asiabill_buy_item_fail,
                            Toast.LENGTH_SHORT).show();
                } else if (TextUtils.equals(code, "6600")) {
                    intent.putExtra(ALIPAY_PAYMENT_RESULT, 0);
                    intent.putExtra(ALIPAY_PAYMENT_ORDERID, ORDERNO);
                    MainActivity.this.startActivity(intent);
                    Toast.makeText(MainActivity.this, R.string.asiabill_buy_item_pending,
                            Toast.LENGTH_SHORT).show();
                } else if (TextUtils.equals(code, "5500")) {
                    Toast.makeText(MainActivity.this, R.string.asiabill_buy_item_canceled,
                            Toast.LENGTH_SHORT).show();
                } else {
                    intent.putExtra(ALIPAY_PAYMENT_RESULT, 0);
                    intent.putExtra(ALIPAY_PAYMENT_ORDERID, ORDERNO);
                    MainActivity.this.startActivity(intent);
                    Toast.makeText(MainActivity.this, R.string.asiabill_buy_item_pending,
                            Toast.LENGTH_SHORT).show();
                }
            }
            getSessionToken();
        }

    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandle.removeCallbacksAndMessages(null);
        mHandle = null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeUi();
        getSessionToken();
    }

    private void initializeUi() {
        // Use view binding to access the UI elements
        layoutBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(layoutBinding.getRoot());
        layoutBinding.detailIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeLayoutVisible();
            }
        });

        layoutBinding.goodDetailOne.placeDetailBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (System.currentTimeMillis() - clickTime > 800) {
                    clickTime = System.currentTimeMillis();
                    viewManagerType = "0";
                    if (CreditCardStr.equals(paymentMethod)) {
                        buildVersionTwoPaymentInfo(layoutBinding.goodDetailOne.price.getText().toString().trim());
                    } else {
                        buildVersionOnePaymentInfo(layoutBinding.goodDetailOne.price.getText().toString().trim());
                    }
                }
            }
        });

        layoutBinding.goodDetailTwo.placeDetailBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (System.currentTimeMillis() - clickTime > 800) {
                    clickTime = System.currentTimeMillis();
                    viewManagerType = "0";
                    if (CreditCardStr.equals(paymentMethod)) {
                        buildVersionTwoPaymentInfo(layoutBinding.goodDetailTwo.price.getText().toString().trim());
                    } else {
                        buildVersionOnePaymentInfo(layoutBinding.goodDetailTwo.price.getText().toString().trim());
                    }
                }
            }
        });
        layoutBinding.goodDetailThree.placeDetailBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (System.currentTimeMillis() - clickTime > 800) {
                    clickTime = System.currentTimeMillis();
                    viewManagerType = "0";
                    if (CreditCardStr.equals(paymentMethod)) {
                        buildVersionTwoPaymentInfo(layoutBinding.goodDetailThree.price.getText().toString().trim());
                    } else {
                        buildVersionOnePaymentInfo(layoutBinding.goodDetailThree.price.getText().toString().trim());
                    }
                }
            }
        });

        layoutBinding.mianCreatcustomerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SESSION_TOKEN != null && !TextUtils.isEmpty(SESSION_TOKEN)) {
                    getCustomers(SESSION_TOKEN);
                }
            }
        });
    }

    /**
     * 商户构建ASIABILL SDK基础数据
     */

    // 商户号
    public static String merNo = "12184";

    // 网关接入号
    public static String gatewayNo = "12184001";

    // 签名密钥
    public static String signKey = "12345678";

    /**
     * Google Pay 商户ID配置说明：
     * 
     * 【测试环境】可不传，SDK会使用gatewayNo作为默认值
     * 【生产环境】必须传入Google Pay Business Console中获取的商户ID
     * 
     * 获取方式：
     * 1. 访问 https://pay.google.com/business/console 注册商户账号
     * 2. 完成Google的集成审核流程
     * 3. 获取格式为 "BCR2DN4TXXXXXXXX" 的商户ID
     * 
     * 注意：即使使用AsiaBill网关集成模式，生产环境仍需向Google注册
     */
    public static String googlepayMerchId = "";// 生产环境必填，测试环境可为空

    /**
     * Google Pay 商户名称配置说明：
     * 
     * 此名称将显示在Google Pay付款页面上，用户可以看到向哪个商户付款
     * 
     * 【建议】设置为您的品牌名称
     * 【示例】"AsiaBill"
     * 
     * 注意：根据Google Pay政策，此名称应与您在Google Pay Business Console注册的商户名称一致
     */
    public static String googlepayMerchName = "";// Google Pay付款页面显示的商户名称

    // 商户订单号
    public static String ORDERNO = String.valueOf(System.currentTimeMillis());

    // 商户交易支付令牌
    public static String SESSION_TOKEN = "";

    // 客户ID
    public static String CUSTOMERID = "";

    // 交易币种
    private String ORDER_CURRENCY = "USD";
    // 交易币种（韩国专用）
    // private String ORDER_CURRENCY = "PHP";
    // private String ORDER_CURRENCY = "KRW";

    // 交易金额
    private String ORDER_AMOUNT = "100";

    // 客人的账单国家
    private String country = "US";
    // 客人的账单国家（韩国专用）
    // private String country="Kr";

    // 0、默认 1、商户自定义-旧卡支付 2、商户自定义-新卡支付
    private String viewManagerType = "2";

    // 后台通知地址
    // private String callbackUrl =
    // "https://testpay.asiabill.com/services/v3/CallResult";
    private String callbackUrl = "https://safepay.asiabill.com/services/v3/CallResult";

    // 支付方式
    private String paymentMethod = "Credit Card"; // Credit Card | CryptoPayment
    // 支付方式（韩国专用）
    // private String paymentMethod = "GCASH";//1.0web支付方式GCASH,韩国专用
    // private String paymentMethod = "KAKAOPAY";//1.0web支付方式KAKAOPAY,韩国专用

    // 是否来源移动设备
    private String isMobile = "2";

    // 16位身份证信息且都为数字,country为ID时必传
    private String creNum = "1234567891234569";

    // 支持的信用卡列表
    private JSONArray getAllowedCardNetworks() {
        return new JSONArray()
                .put("Visa")
                .put("Master card")
                .put("American Express")
                .put("JCB")
                .put("Discover")
                .put("Maestro")
                .put("Dinners club");
    }

    /**
     * 海外本地支付支付的入口
     */
    private void buildVersionOnePaymentInfo(String price) {
        ORDERNO = String.valueOf(System.currentTimeMillis());
        ORDER_AMOUNT = "100";
        PayInfoBean payInfoBean = new PayInfoBean.Builder()
                // 设置商户号
                .setMerNo(merNo)
                // 设置网关接入号
                .setGatewayNo(gatewayNo)
                // 设置签名密钥
                .setSignkey(signKey)
                // 设置用户firstName
                .setFirstName("CL")
                // 设置用户lastName
                .setLastName("BRW1")
                // 设置用户email
                .setEmail("string0524@gmail.com")
                // 设置用户phone
                .setPhone("+1 650-555-5555")
                // 设置用户国家
                .setCountry(country)
                // 设置用户所在州
                .setState("CA")
                // 设置用户账单城市
                .setCity("Mountain View")
                // 设置用户账单地址
                .setAddress("1600 Amphitheatre Parkway")
                // 设置用户账单邮编
                .setZip("94043")
                // 设置支付订单号
                .setOrderNo(ORDERNO)
                // 设置支付金额大小
                .setOrderAmount(ORDER_AMOUNT)
                // 设置支付货币单位
                .setOrderCurrency(ORDER_CURRENCY)
                // 设置支付方式
                .setPaymentMethod(paymentMethod)
                // 设置用户身份证号
                // payInfoBean.setCreNum(creNum);
                .build();
        Runnable payRunable = new Runnable() {
            @Override
            public void run() {
                PayTask payTask = new PayTask(MainActivity.this);
                String result = payTask.pay(payInfoBean);

                Message msg = new Message();
                msg.obj = result;
                mHandle.sendMessage(msg);
            }
        };
        Thread payThread = new Thread(payRunable);
        payThread.start();
    }

    /**
     * version asiabill_2.0
     * 国际信用卡支付的入口
     */
    private void buildVersionTwoPaymentInfo(String price) {
        ORDERNO = String.valueOf(System.currentTimeMillis());
        ORDER_AMOUNT = price;

        // 构建商品详情，可不传
        List<GoodsDetail> list = new ArrayList<>();
        list.add(new GoodsDetail("10", "product one", "5"));
        list.add(new GoodsDetail("10.6", "product two", "5"));
        list.add(new GoodsDetail("20.2", "product three", "5"));

        // 构建支付界面头部和请求框资源文件，可不传
        PaymentUiData paymentUiData = new PaymentUiData();
        // paymentUiData.setBackResource(R.drawable.ic_arrow_back);
        // paymentUiData.setTextColor(Color.parseColor("#ffffff"));
        // paymentUiData.setTextSize(18.0f);
        // paymentUiData.setToolbarBackgroundColor(Color.parseColor("#ff0099cc"));
        paymentUiData.setDialogLayoutResource(R.layout.main_dialog_loading);

        /**
         * 构建sdk基础数据类
         * SESSION_TOKEN 【会话令牌】 此ID在有效期内可用，将用于后续的支付处理流程 必传
         */

        PayInfoBean payInfoBean = new PayInfoBean.Builder(SESSION_TOKEN)
                // 设置商户号
                .setMerNo(merNo)
                // 设置网关接入号
                .setGatewayNo(gatewayNo)
                // 设置签名密钥
                .setSignkey(signKey)
                // 设置商品详情，可不传
                .setGoodsDetail(list)
                // 设置支付界面头部和请求框资源文件，可不传
                .setPaymentUiData(paymentUiData)
                // 设置用户firstName
                .setFirstName("CL")
                // 设置用户lastName
                .setLastName("BRW1")
                // 设置用户email
                .setEmail("532539937@qq.com")
                // 设置用户phone
                .setPhone("+1 650-555-5555")
                // 设置用户国家
                .setCountry(country)
                // 设置用户所在州
                .setState("CA")
                // 设置用户账单城市
                .setCity("Mountain View")
                // 设置用户账单地址
                .setAddress("1600 Amphitheatre Parkway")
                // 设置用户账单邮编
                .setZip("94043")
                // 设置支付订单号
                .setOrderNo(ORDERNO)
                // 设置支付金额大小
                .setOrderAmount(ORDER_AMOUNT)
                // 设置支付货币单位
                .setOrderCurrency(ORDER_CURRENCY)
                // 设置支持卡种类型
                .setCardType(getAllowedCardNetworks().toString())
                // 设置支付方式
                .setPaymentMethod(paymentMethod)
                // payInfoBean.setCreNum(creNum);
                // 设置支付方式是否显示googlepay选项（默认为1不显示）；0谷歌 1其他
                .setTokenPayType("0")
                // 【Google Pay商户ID】生产环境必传，测试环境可不传
                // 获取方式：在Google Pay Business Console (https://pay.google.com/business/console)
                // 注册并完成审核后获取
                .setGooglepayMerchId(googlepayMerchId)
                // 【Google Pay商户名称】显示在付款页面，建议设置为您的品牌名称
                .setGooglepayMerchName(googlepayMerchName)
                // 是否开启显示sdk保存卡功能(不传值则不开启) 0：sdk托管显示；1：商户自定义显示ui并且用户已经选择了一张卡；2：商户自定义ui并且选择了添加新卡
                .setViewManagerType(viewManagerType)
                // 设置用户的customerId
                .setCustomerID(CUSTOMERID)
                // 商户后台异步通知交易结果使用该URL通知
                .setCallbackUrl(callbackUrl)
                .build();
        Runnable payRunable = new Runnable() {
            @Override
            public void run() {
                PayTask payTask = new PayTask(MainActivity.this);
                String result = payTask.pay(payInfoBean);

                Message msg = new Message();
                msg.obj = result;
                mHandle.sendMessage(msg);
            }
        };
        Thread payThread = new Thread(payRunable);
        payThread.start();
    }

    /**
     * version asiabill_2.1
     * 商户自定义ui需要走3D支付方法
     */
    private void payThreeD(PayInfoBean payInfoBean) {
        String threeDUrl = "https://192.168.3.126:8080/services/v3/threeDsCheck?threeDsParam=1f77fa7fd70c308fe66bc406cfd1df0d69b974e1fe6304c480b436182ec803fb3b0f29f6b4dfe17f8bd4aafff52308f3a87c7cd3b7b74e06b3ee709ad2794a70aaa317fa9a753a635fcc5b9427db00cc5e7f1b632acd8931a7cf3317fecb9dabb0a200a7cd6ae8c6cb3deee49b8bf0390b2b671e158cf961f5a85cdf1c9a7ecaa4292678f3a13968f1067bc3fb3dc4c13187097f836b5cd175ad14adc264b9cda391097953c667f9080eaa7a256734c0a39e2e222961c2cab8675a2ecc6f7dc081106286ac2117643bc67d744f7bf31e38c95759307a41a0296fa3b425f4144147df63cb6fda9c656507245466c83d20cc683adbc1ec9d8aaffe9c0eeaabd64d5a25d877eb8e36ed31ddab8710a45e1fe46e03b0dad78eee792ab1e83ea2d92d96f8b5501ff46d7c53d0ce7f5adee9e34ddc79faf87cf3d7fdfb9dba00d8a13c2669301e7dc82a8fe0a1fc3847c95698a604dcab22d59c83d6042c982f15199f96c82dc235c6968e9596e680efd6b51b030a359cbc459310fd71136dcd2f49cda391097953c667f9080eaa7a256734c085039570073f081ce1648496e6cf28b88d12abbf79652611a03549ee34bfd36990f0b896dd5e85673132ea2e84d2bbca74f4db45c7f62f740d9a002f1b206dca&threeDsType=9";
        if (!TextUtils.isEmpty(threeDUrl)) {
            payThreeDCheck(threeDUrl, payInfoBean);
        }
    }

    /**
     * version asiabill_2.1
     * 商户自定义ui需要走3D支付流程时验证入口
     *
     * @param url 3D验证url地址
     */
    private void payThreeDCheck(String url, PayInfoBean payInfoBean) {
        Runnable payRunnable = new Runnable() {
            @Override
            public void run() {
                PayTask payTask = new PayTask(MainActivity.this);
                String result = payTask.payThreeDCheck(url, payInfoBean);

                Message msg = new Message();
                msg.obj = result;
                mHandle.sendMessage(msg);
            }
        };
        Thread payThread = new Thread(payRunnable);
        payThread.start();
    }

    public void changeLayoutVisible() {
        if (layoutBinding.placeHolderLayout.getVisibility() == View.VISIBLE) {
            layoutBinding.placeHolderLayout.setVisibility(View.GONE);
        } else {
            layoutBinding.placeHolderLayout.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 获取订单交易令牌sessionToken
     */
    public void getSessionToken() {
        SessionTokenRequest mSessionTokenRequest = PayTask.getSessionTokenRequest(merNo, gatewayNo, signKey);
        String BaseUrl = BASE_TPO_URL;
        if (PayTask.getPaymentEnvironment() == PaymentEnvironment.TEST) {
            BaseUrl = BASE_TPO_URL_TEST;
        }
        AsiaBillRetrofitClient.getInstance(BaseUrl, 10).create(PaymentApiService.class)
                .gainSessionToken(mSessionTokenRequest, CRAET_SESSION_TOKEN)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<BaseResponse<SessionTokenResponse>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onNext(@NonNull BaseResponse<SessionTokenResponse> sessionTokenResponse) {
                        if (sessionTokenResponse.getCode().equals("0") &&
                                sessionTokenResponse.getData() != null) {
                            SESSION_TOKEN = sessionTokenResponse.getData().getSessionToken();
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    /**
     * 获取订单交易令牌sessionToken
     */
    public void getCustomers(String token) {
        CreateCustomerRequest mCreateCustomerRequest = new CreateCustomerRequest();
        // description String 50 No 【描述】
        mCreateCustomerRequest.setDescription("desc");
        // email String 200 No 【邮箱】
        mCreateCustomerRequest.setEmail("string0524@gmail.com");
        // firstName String 50 No 【姓】
        mCreateCustomerRequest.setFirstName("zhang");
        // lastName String 50 No 【名】
        mCreateCustomerRequest.setLastName("sang");
        // phone String 50 No 【电话】
        mCreateCustomerRequest.setPhone("18126541234");
        String BaseUrl = BASE_TPO_URL;
        if (PayTask.getPaymentEnvironment() == PaymentEnvironment.TEST) {
            BaseUrl = BASE_TPO_URL_TEST;
        }
        AsiaBillRetrofitClient.getInstance(BaseUrl, 10).create(PaymentApiService.class)
                .createCustomer(mCreateCustomerRequest, CRAET_CUSTOMERS, token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<BaseResponse<CreateCustomerResponse>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onNext(@NonNull BaseResponse<CreateCustomerResponse> sessionTokenResponse) {
                        if (sessionTokenResponse != null && sessionTokenResponse.getCode().equals("0") &&
                                sessionTokenResponse.getData() != null) {
                            if (sessionTokenResponse.getData().getCustomerId() != null) {
                                CUSTOMERID = sessionTokenResponse.getData().getCustomerId();
                                Toast toast = Toast.makeText(MainActivity.this,
                                        R.string.asiabill_checkout_creat_customer_success,
                                        Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER, 0, 0);
                                toast.show();
                            }
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }
}
