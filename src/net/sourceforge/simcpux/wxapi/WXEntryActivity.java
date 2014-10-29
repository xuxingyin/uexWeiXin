package net.sourceforge.simcpux.wxapi;

import org.zywx.wbpalmstar.plugin.uexweixin.EuexWeChat;
import org.zywx.wbpalmstar.plugin.uexweixin.Utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.tencent.mm.sdk.constants.ConstantsAPI;
import com.tencent.mm.sdk.modelbase.BaseReq;
import com.tencent.mm.sdk.modelbase.BaseResp;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

public class WXEntryActivity extends Activity implements IWXAPIEventHandler {

	private IWXAPI api;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		api = WXAPIFactory.createWXAPI(this, Utils.getAppId(this));
		api.registerApp(Utils.getAppId(this));
		api.handleIntent(getIntent(), this);

	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		api.handleIntent(getIntent(), this);
	}

	@Override
	public void onReq(BaseReq req) {
		switch (req.getType()) {
		case ConstantsAPI.COMMAND_GETMESSAGE_FROM_WX:
			break;
		case ConstantsAPI.COMMAND_SENDAUTH:
			break;
		case ConstantsAPI.COMMAND_SENDMESSAGE_TO_WX:
			break;
		case ConstantsAPI.COMMAND_SHOWMESSAGE_FROM_WX:
			break;
		case ConstantsAPI.COMMAND_UNKNOWN:
			break;
		default:
			break;
		}

	}

	@Override
	public void onResp(final BaseResp resp) {
		int statusCode = 0;
		Log.i("resp", resp.getType() + "----------");
		switch (resp.errCode) {
		case BaseResp.ErrCode.ERR_AUTH_DENIED:
			statusCode = -4;
			break;
		case BaseResp.ErrCode.ERR_COMM:
			statusCode = -1;
			break;
		case BaseResp.ErrCode.ERR_OK:
			statusCode = 0;
			break;
		case BaseResp.ErrCode.ERR_SENT_FAILED:
			statusCode = -3;
			break;

		case BaseResp.ErrCode.ERR_UNSUPPORT:
			statusCode = -5;
			break;
		case BaseResp.ErrCode.ERR_USER_CANCEL:
			statusCode = -2;
		default:
			break;
		}
		if (EuexWeChat.weChatCallBack != null) {
			EuexWeChat.weChatCallBack.callBackShareResult(statusCode);
		}
		finish();
	}
}
