package org.zywx.wbpalmstar.plugin.uexweixin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.tencent.mm.sdk.constants.Build;
import com.tencent.mm.sdk.modelbase.BaseResp;
import com.tencent.mm.sdk.modelmsg.SendMessageToWX;
import com.tencent.mm.sdk.modelmsg.WXImageObject;
import com.tencent.mm.sdk.modelmsg.WXMediaMessage;
import com.tencent.mm.sdk.modelmsg.WXTextObject;
import com.tencent.mm.sdk.modelmsg.WXWebpageObject;
import com.tencent.mm.sdk.modelpay.PayReq;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

@SuppressLint("SdCardPath")
public class EuexWeChat extends EUExBase {

	private static final String TAG = "EuexWeChat";

	public static final String CB_REGISTER_WXAPP_RESULT = "uexWeiXin.cbRegisterApp";//注册回调
	public static final String CB_IS_WXAPP_INSTALLIED = "uexWeiXin.cbIsWXAppInstalled";//
	public static final String CB_SEND_TEXT_CONTENT = "uexWeiXin.cbSendTextContent";//
	public static final String CB_SEND_IMAGE_CONTENT = "uexWeiXin.cbSendImageContent";
	public static final String CB_IS_PAY_SUPPORTED = "uexWeiXin.cbIsSupportPay";
	public static final String CB_GET_ACCESS_TOKEN = "uexWeiXin.cbGetAccessToken";
	public static final String CB_GET_PREPAY_ID = "uexWeiXin.cbGenerateAdvanceOrder";
	public static final String CB_GET_PAY_RESULT = "uexWeiXin.cbGotoPay";
	public static final String CB_GET_ACCESS_TOKEN_LOCAL = "uexWeiXin.cbGetAccessTokenLocal";

	private static final int THUMB_SIZE = 100;

	private static final int TEXT_CODE = 1;
	private static final int IMAGE_CODE = 2;

	private int code;

	private IWXAPI api;
	public static WeChatCallBack weChatCallBack;
	private String appId;
	private long timeStamp;
	private String nonceStr;
	

	public EuexWeChat(Context context, EBrowserView parent) {
		super(context, parent);
		init();
	}

	public String getJson(String errCode, String errStr) {
		String myString;
		if (errStr == null) {
			errStr = "";
		}
		try {
			myString = new JSONStringer().object().key("errCode")
					.value(errCode).key("errStr").value(errStr).endObject()
					.toString();
			Log.d("path", myString);
			return myString;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void init() {
		weChatCallBack = new WeChatCallBack() {
			@Override
			public void callBackPayResult(BaseResp msg) {
				jsCallbackAsyn(CB_GET_PAY_RESULT, 0, EUExCallback.F_C_JSON,
                        getJson(msg.errCode + "", msg.errStr));
			}

			@Override
			public void callBackShareResult(int message) {
				if (code == 1) {//类型代表是文字类型
                    jsCallbackAsyn(CB_SEND_TEXT_CONTENT, 0, EUExCallback.F_C_INT,
                          message==0?"0":"1");
				} else if (code == 2) {//类型代表是图片类型
                    jsCallbackAsyn(CB_SEND_IMAGE_CONTENT, 0, EUExCallback.F_C_INT,
                          message==0?"0":"1");
				}
				Log.d("resp", message + "---3---");
				System.out.println(message + "yyyyyyyy");
			}
		};
	}

	/**
	 * 注册应用到微信
	 * 
	 * @param appId
	 *            从微信开放平台申请的appId
	 * @return true 注册成功， false 注册失败
	 */
	public boolean registerApp(String[] data) {
		Log.d(TAG, "registerApp");
		if (data == null || data.length < 1) {
			return false;
		}
		appId = data[0];
		if (appId == null || appId.length() == 0) {
			return false;
		}
		api = WXAPIFactory.createWXAPI(mContext, appId, false);
		boolean regOk = api.registerApp(appId);
		if (regOk) {
			Utils.setAppId(mContext, appId);
		}
		jsCallback(CB_REGISTER_WXAPP_RESULT, 0, EUExCallback.F_C_INT, regOk?0:1);//注册回调 0-成功 1-失败
		return regOk;
	}

	public boolean sendTextContent(String[] params) {
		Log.d(TAG, "sendTextContent");
		if (params == null || params.length < 2) {
			return false;
		}
		code = TEXT_CODE;
		try {
			int scene = Integer.parseInt(params[0]);
			String text = params[1];
			return sendTextContent(scene, text);
		} catch (Exception e) {
			Toast.makeText(mContext, "参数错误： " + params[0], Toast.LENGTH_SHORT)
					.show();
		}
		return false;
	}

	public boolean sendImageContent(String[] params) {
		Log.d(TAG, "sendImageContent");
		if (params == null || params.length < 3) {
			return false;
		}
		code = IMAGE_CODE;
		try {
			int scene = Integer.parseInt(params[0]);
			String thumbPath = params[1];
			String imgPath = params[2];
			String imageUrl = params[3];
			String imageTitle = params[4];
			String imageDescription = params[5];
			return sendImageContent(scene, thumbPath, imgPath, imageTitle,
					imageDescription, imageUrl);
		} catch (Exception e) {
			Toast.makeText(mContext, "参数错误：" + e.getMessage(),
					Toast.LENGTH_SHORT).show();
		}
		return false;
	}

	/**
	 * 判断是否安装微信应用，
	 * 
	 * @return true 已安装， false 未安装
	 */
	public boolean isWXAppInstalled(String[] args) {
		Log.d(TAG, "isWXAppInstalled");
		// 判断是否安装微信，安装返回true。
		boolean isWXInstalled = api.isWXAppInstalled();
		jsCallback(CB_IS_WXAPP_INSTALLIED, 0, EUExCallback.F_C_TEXT,
				isWXInstalled ? 0 : 1);
		return isWXInstalled;
	}

	/**
	 * 
	 * @param scene
	 * @param text
	 * @return
	 */
	public boolean sendTextContent(int scene, String text) {
		if (text == null || text.length() == 0) {
			return false;
		}

		WXTextObject textObj = new WXTextObject();
		textObj.text = text;

		WXMediaMessage msg = new WXMediaMessage();
		msg.description = text;
		msg.mediaObject = textObj;

		SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.message = msg;
		req.scene = scene;
		req.transaction = "text";
		Log.i(TAG, scene + " " + text);
		return api.sendReq(req);

	}

	/**
	 * 
	 * @param scene
	 *            发送场景，0 微信， 1 朋友圈
	 * @param thumImgPath
	 *            缩略图地址
	 * @param realImgPath
	 *            图片地址
	 * @return true 发送成功， false 发送失败
	 */
	public boolean sendImageContent(int scene, String thumbImgPath,
			String realImgPath, String imageTitle, String imageDescription,
			String imageUrl) {

		if (realImgPath == null || realImgPath.length() == 0) {
			return false;
		}

		WXImageObject imgObj = new WXImageObject();
		String imgPath;
		String thumbPath;

		if (realImgPath.startsWith("http://")) {
			imgPath = realImgPath;
			imgObj.imageUrl = imgPath;
		} else {
			imgPath = getAbsPath(realImgPath);
			if (realImgPath.startsWith("/")) {
				imgObj.imagePath = imgPath;
			} else {
				try {
					InputStream in = mContext.getResources().getAssets()
							.open(imgPath);
					int lenght = in.available();
					BitmapFactory.Options opts = null;
					int count = 0;
					count = lenght / 100000 + 1;
					opts = new BitmapFactory.Options();
					if (lenght > 100000) {
						opts.inSampleSize = count;
					} else {
						opts.inSampleSize = 1;
					}
					Bitmap bmp = BitmapFactory.decodeStream(mContext
							.getAssets().open(imgPath));
					imgObj.imageData = Utils.bmpToByteArray(bmp, true);
				} catch (Exception e) {
					Toast.makeText(mContext, "图片不存在： " + e.getMessage(),
							Toast.LENGTH_SHORT).show();
				}
			}
		}

		// 缩略图地址临时变量
		Bitmap bmp = null;

		// 如果没有缩略图地址，图片地址赋值给缩略图地址，生成缩略图
		if (thumbImgPath == null || thumbImgPath.length() == 0) {
			thumbPath = imgPath;
		} else {// 传递缩略图地址
			thumbPath = thumbImgPath;
		}

		if (thumbPath.startsWith("http://")) {
			try {
				bmp = BitmapFactory.decodeStream(new URL(thumbPath)
						.openStream());
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			if (thumbPath.startsWith("/")) {// sd卡路径时
				File f = new File(thumbPath);
				if (!f.exists()) {
					Toast.makeText(mContext, "File is not exist!",
							Toast.LENGTH_SHORT).show();
				}
				bmp = BitmapFactory.decodeFile(thumbPath);
			} else {
				try {

					bmp = BitmapFactory.decodeStream(mContext.getAssets().open(
							getAbsPath(thumbPath)));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		Bitmap thumbBmp = Bitmap.createScaledBitmap(bmp, THUMB_SIZE,
				THUMB_SIZE, true);
		if (bmp != null) {
			bmp.recycle();
		}

		WXMediaMessage msg = new WXMediaMessage();
		msg.thumbData = Utils.bmpToByteArray(thumbBmp, true);
		msg.title = imageTitle;
		msg.description = imageDescription;
		if (imageUrl == null || imageUrl.length() == 0) {
			msg.mediaObject = imgObj;
		} else {
			WXWebpageObject webObj = new WXWebpageObject();
			webObj.webpageUrl = imageUrl;
			msg.mediaObject = webObj;
		}

		SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.transaction = buildTransaction("img");
		req.message = msg;
		req.scene = scene;
		Log.i(TAG, scene + " " + realImgPath);
		return api.sendReq(req);
	}

	public final static String F_SDCARD_PATH = "file:///sdcard/";
	public final static String F_RES_PATH = "file:///res/";
	public final static String F_DATA_PATH = "file:///data/";
	public final static String F_HTTP_PATH = "http://";
	public final static String F_RTSP_PATH = "rtsp://";
	public final static String F_RES_ROOT_PATH = "file:///res/widget/";
	public final static String F_ASSET_PATH = "file:///android_asset/";
	public final static String F_RAW_PATH = "raw/";
	public final static String F_FILE_SCHEMA = "file://";
	public final static String F_APP_SCHEMA = "wgt://";
	public final static String F_WIDGET_SCHEMA = "wgts://";
	public final static String F_BASE_WGT_PATH = "widgetone/";
	public final static String F_APP_PATH = "widgetone/apps/";
	public final static String F_WIDGET_PATH = "widgetone/widgets/";
	public final static String F_WIDGET_APP_PATH = "widgetone/widgetapp/";
	public final static String F_APP_VIDEO = "video/";
	public final static String F_APP_PHOTO = "photo/";
	public final static String F_APP_AUDIO = "audio/";
	public final static String F_APP_MYSPACE = "myspace/";
	public final static String F_Widget_RES_path = "widget/wgtRes/";
	public final static String F_Widget_RES_SCHEMA = "res://";
	public final static String F_SBOX_SCHEMA = "box://";

	private String getAbsPath(String path) {

		if (path == null || path.length() == 0) {
			return null;
		}
		if (path.startsWith(F_ASSET_PATH)) {
			return path.substring(F_ASSET_PATH.length());
		} else if (path.startsWith(F_FILE_SCHEMA)) {
			return path.substring(F_FILE_SCHEMA.length());
		}
		if (path.startsWith(F_Widget_RES_SCHEMA)) {
			return F_Widget_RES_path
					+ path.substring(F_Widget_RES_SCHEMA.length());
		} else {
			return path;
		}
	}

	private String buildTransaction(final String type) {
		return (type == null) ? String.valueOf(System.currentTimeMillis())
				: type + System.currentTimeMillis();
	}

	public void isSupportPay(String[] params) {
		// getWXAppSupportSAPI是否支持微信版本。
		boolean isPaySupported = api.getWXAppSupportAPI() >= Build.PAY_SUPPORTED_SDK_INT;
		// isPaySupported判断是否支持已安装的微信版本。
		if (isPaySupported) {
			jsCallback(CB_IS_PAY_SUPPORTED, 0, EUExCallback.F_C_INT, 0);
		} else {
			jsCallback(CB_IS_PAY_SUPPORTED, 0, EUExCallback.F_C_INT, 1);
		}
	}

	public void getAccessToken(String[] params) {
		new GetAccessTokenTask(params[0], params[1]).execute();
	}

	String contentAccess = "";

	private class GetAccessTokenTask extends
			AsyncTask<Void, Void, GetAccessTokenResult> {

		private ProgressDialog dialog;
		private String appId, appSecret;

		public GetAccessTokenTask(String mAppId, String mAppSecret) {
			this.appId = mAppId;
			this.appSecret = mAppSecret;
		}

		@Override
		protected void onPreExecute() {
			dialog = ProgressDialog.show(mContext, "提示", "正在获取access token...");
		}

		@Override
		protected void onPostExecute(GetAccessTokenResult result) {
			if (dialog != null) {
				dialog.dismiss();
			}

			if (result.localRetCode == LocalRetCode.ERR_OK) {
				jsCallback(CB_GET_ACCESS_TOKEN, 0, EUExCallback.F_C_TEXT,
						contentAccess);

			} else {
				jsCallback(CB_GET_ACCESS_TOKEN, 0, EUExCallback.F_C_TEXT,
						result.localRetCode.name());
			}
		}

		@Override
		protected GetAccessTokenResult doInBackground(Void... params) {
			GetAccessTokenResult result = new GetAccessTokenResult();
			// String.format用与替换路径后面的%s。
			String url = String
					.format("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s",
							appId, appSecret);
			byte[] buf = Utils.httpGet(url);
			if (buf == null || buf.length == 0) {
				result.localRetCode = LocalRetCode.ERR_HTTP;
				return result;
			}
			contentAccess = new String(buf);
			// *****************************
			// 用于将获取到的Token数据，存储到本地。
			SharedPreferences.Editor token = mContext.getSharedPreferences(
					"token", 0).edit();
			token.putString("token", contentAccess);
			token.commit();
			System.out.println(contentAccess);

			result.parseFrom(contentAccess);
			return result;
		}
	}

	// 获取本地存储的Token数据。
	public void getAccessTokenLocal(String[] params) {
		setCallBackData();
	}

	String contentAccessLocal = "";

	public void setCallBackData() {
		if (getLocalData().localRetCode == LocalRetCode.ERR_OK) {
			jsCallback(CB_GET_ACCESS_TOKEN_LOCAL, 0, EUExCallback.F_C_TEXT,
					contentAccessLocal);

		} else {
			jsCallback(CB_GET_ACCESS_TOKEN_LOCAL, 0, EUExCallback.F_C_TEXT,
					getLocalData().localRetCode.name());
		}
	}

	public GetAccessTokenResult getLocalData() {
		GetAccessTokenResult result = new GetAccessTokenResult();
		// 获取本地存储的Token数据
		SharedPreferences sharedata = mContext.getSharedPreferences("token", 0);
		String data = sharedata.getString("token", null);
		System.out.println(data + "-------");
		byte[] buf = data.getBytes();
		if (buf == null || buf.length == 0) {
			result.localRetCode = LocalRetCode.ERR_OTHER;
			return result;
		}
		contentAccessLocal = new String(buf);
		System.out.println(contentAccessLocal);

		result.parseFrom(contentAccessLocal);
		return result;
	}

	public void generateAdvanceOrder(String[] params) {
		System.out.println("params[0]=" + params[0]);
		System.out.println("params[1]=" + params[1]);
		GetPrepayIdTask getPrepayId = new GetPrepayIdTask(params[0], params[1]);
		getPrepayId.execute();
	}

    public void generatePrepayID(String[] params) {
        GeneratePrepayID getPrepayId = new GeneratePrepayID(params);
        getPrepayId.execute();
    }

	private static class GetPrepayIdResult {
		public LocalRetCode localRetCode = LocalRetCode.ERR_OTHER;

		public void parseFrom(String content) {

			if (content == null || content.length() <= 0) {
				localRetCode = LocalRetCode.ERR_JSON;
				return;
			}
			try {
				JSONObject json = new JSONObject(content);
				if (json.has("prepayid")) { // success case
					localRetCode = LocalRetCode.ERR_OK;
				} else {
					localRetCode = LocalRetCode.ERR_JSON;
				}
			} catch (Exception e) {
				localRetCode = LocalRetCode.ERR_JSON;
			}
		}
	}

	String jsonPrepayData = "";

	private class GetPrepayIdTask extends
			AsyncTask<Void, Void, GetPrepayIdResult> {

		private ProgressDialog dialog;
		private String accessToken, entity;

		public GetPrepayIdTask(String mAccessToken, String mEntity) {
			this.accessToken = mAccessToken;
			this.entity = mEntity;
		}

		@Override
		protected void onPreExecute() {
			dialog = ProgressDialog.show(mContext, "提示", "正在获取预支付订单...");
		}

		@Override
		protected void onPostExecute(GetPrepayIdResult result) {
			if (dialog != null) {
				dialog.dismiss();
			}

			if (result.localRetCode == LocalRetCode.ERR_OK) {
				jsCallback(CB_GET_PREPAY_ID, 0, EUExCallback.F_C_TEXT,
						jsonPrepayData);
			} else {
				jsCallback(CB_GET_PREPAY_ID, 0, EUExCallback.F_C_TEXT,
						result.localRetCode.name());
			}
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
		}

		@Override
		protected GetPrepayIdResult doInBackground(Void... params) {

			String url = String.format(
					"https://api.weixin.qq.com/pay/genprepay?access_token=%s",
					accessToken);

			Log.d(TAG, "doInBackground, url = " + url);
			Log.d(TAG, "doInBackground, entity = " + entity);

			GetPrepayIdResult result = new GetPrepayIdResult();

			byte[] buf = Utils.httpPost(url, entity);

			// ****************************************

			if (buf == null || buf.length == 0) {
				result.localRetCode = LocalRetCode.ERR_HTTP;
				return result;
			}
			// 获取预付网络Json数据，
			jsonPrepayData = new String(buf);
			String content = new String(buf);
			Log.d(TAG, "doInBackground, content = " + content);
			result.parseFrom(content);
			return result;
		}
	}

    private class GeneratePrepayID extends
            AsyncTask<Void, Void, GetPrepayIdResult> {
        
        private ProgressDialog dialog;
        private String accessToken, appKey, packageValue, traceId;
        
        public GeneratePrepayID(String[] params) {
            this.accessToken = params[0];
            this.appKey = params[1];
            packageValue = params[2];
            this.traceId = getTraceId();
            if(params.length > 3){
                this.traceId = params[3];
            }
        }
        
        @Override
        protected void onPreExecute() {
            dialog = ProgressDialog.show(mContext, "提示", "正在获取预支付订单...");
        }
        
        @Override
        protected void onPostExecute(GetPrepayIdResult result) {
            if (dialog != null) {
                dialog.dismiss();
            }
        
            if (result.localRetCode == LocalRetCode.ERR_OK) {
                jsCallback(CB_GET_PREPAY_ID, 0, EUExCallback.F_C_TEXT,
                        jsonPrepayData);
            } else {
                jsCallback(CB_GET_PREPAY_ID, 0, EUExCallback.F_C_TEXT,
                        result.localRetCode.name());
            }
        }
        
        @Override
        protected void onCancelled() {
            super.onCancelled();
        }
        
        @Override
        protected GetPrepayIdResult doInBackground(Void... params) {
        
            String url = String.format(
                    "https://api.weixin.qq.com/pay/genprepay?access_token=%s",
                    accessToken);
        
            Log.d(TAG, "doInBackground, url = " + url);
            String entity = genProductArgs(appKey, packageValue, traceId);
            GetPrepayIdResult result = new GetPrepayIdResult();
        
            byte[] buf = Utils.httpPost(url, entity);
        
            // ****************************************
        
            if (buf == null || buf.length == 0) {
                result.localRetCode = LocalRetCode.ERR_HTTP;
                return result;
            }
            // 获取预付网络Json数据，
            jsonPrepayData = new String(buf);
            String content = new String(buf);
            Log.d(TAG, "doInBackground, content = " + content);
            result.parseFrom(content);
            return result;
        }
    }

    private String genProductArgs(String appKey, String packageValue, String traceId) {
        JSONObject json = new JSONObject();
        
        try {
            json.put("appid", appId);
            if(!TextUtils.isEmpty(traceId)){
                json.put("traceid", traceId); 
            }
            nonceStr = genNonceStr();
            json.put("noncestr", nonceStr);
            json.put("package", packageValue);
            timeStamp = genTimeStamp();
            json.put("timestamp", timeStamp);
            
            List<NameValuePair> signParams = new LinkedList<NameValuePair>();
            signParams.add(new BasicNameValuePair("appid", appId));
            signParams.add(new BasicNameValuePair("appkey", appKey));
            signParams.add(new BasicNameValuePair("noncestr", nonceStr));
            signParams.add(new BasicNameValuePair("package", packageValue));
            signParams.add(new BasicNameValuePair("timestamp", String.valueOf(timeStamp)));
            signParams.add(new BasicNameValuePair("traceid", traceId));
            json.put("app_signature", genSign(signParams));
            
            json.put("sign_method", "sha1");
        } catch (Exception e) {
            Log.e(TAG, "genProductArgs fail, ex = " + e.getMessage());
            return null;
        }
        
        return json.toString();
    }

    public void sendPay(String[] params) {
        PayReq req = new PayReq();
        req.appId = appId;
        req.partnerId = params[0];
        req.prepayId = params[1];
        req.nonceStr = nonceStr;
        req.timeStamp = String.valueOf(timeStamp);
        req.packageValue = "Sign=" + params[3];
        
        List<NameValuePair> signParams = new LinkedList<NameValuePair>();
        signParams.add(new BasicNameValuePair("appid", req.appId));
        signParams.add(new BasicNameValuePair("appkey", params[2]));
        signParams.add(new BasicNameValuePair("noncestr", req.nonceStr));
        signParams.add(new BasicNameValuePair("package", req.packageValue));
        signParams.add(new BasicNameValuePair("partnerid", req.partnerId));
        signParams.add(new BasicNameValuePair("prepayid", req.prepayId));
        signParams.add(new BasicNameValuePair("timestamp", req.timeStamp));
        req.sign = genSign(signParams);

        // 在支付之前，如果应用没有注册到微信，应该先调用IWXMsg.registerApp将应用注册到微信
        api.sendReq(req);
    }  
    
	public void gotoPay(String[] params) {
	    PayReq req = new PayReq();
		req.appId = Utils.getAppId(mContext);
		req.partnerId = params[0];
		req.prepayId = params[1];
		req.packageValue = params[2];
		req.nonceStr = params[3];
		req.timeStamp = params[4];
        req.sign = params[5];
        api.sendReq(req);
	}

	private String genSign(final List<NameValuePair> params) {
		StringBuilder sb = new StringBuilder();

		int i = 0;
		for (; i < params.size() - 1; i++) {
			sb.append(params.get(i).getName());
			sb.append('=');
			sb.append(params.get(i).getValue());
			sb.append('&');
		}
		sb.append(params.get(i).getName());
		sb.append('=');
		sb.append(params.get(i).getValue());

		String sha1 = Utils.sha1(sb.toString());
		return sha1;
	}

	private long genTimeStamp() {
		return System.currentTimeMillis() / 1000;
	}

	/**
	 * 建议 traceid 字段包含用户信息及订单信息，方便后续对订单状态的查询和跟踪
	 */
	private String getTraceId() {
		return "crestxu_" + genTimeStamp();
	}

	private String genNonceStr() {
		Random random = new Random();
		return MD5.getMessageDigest(String.valueOf(random.nextInt(10000))
				.getBytes());
	}

	private static class GetAccessTokenResult {

		public LocalRetCode localRetCode = LocalRetCode.ERR_OTHER;

		public void parseFrom(String content) {

			if (content == null || content.length() <= 0) {
				localRetCode = LocalRetCode.ERR_JSON;
				return;
			}

			try {
				JSONObject json = new JSONObject(content);
				if (json.has("access_token")) { // success case
					localRetCode = LocalRetCode.ERR_OK;
				} else {
					localRetCode = LocalRetCode.ERR_JSON;
				}

			} catch (Exception e) {
				localRetCode = LocalRetCode.ERR_JSON;
			}
		}
	}

	private static enum LocalRetCode {
		ERR_OK, ERR_HTTP, ERR_JSON, ERR_OTHER
	}

	@Override
	protected boolean clean() {
		return false;
	}

	public interface WeChatCallBack {
		void callBackPayResult(BaseResp msg);

		void callBackShareResult(int message);

	}

}
