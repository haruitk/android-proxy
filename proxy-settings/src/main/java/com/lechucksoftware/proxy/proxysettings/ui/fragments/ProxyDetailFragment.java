package com.lechucksoftware.proxy.proxysettings.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import com.lechucksoftware.proxy.proxysettings.App;
import com.lechucksoftware.proxy.proxysettings.R;
import com.lechucksoftware.proxy.proxysettings.db.ProxyEntity;
import com.lechucksoftware.proxy.proxysettings.ui.activities.ProxyDetailActivity;
import com.lechucksoftware.proxy.proxysettings.ui.components.InputExclusionList;
import com.lechucksoftware.proxy.proxysettings.ui.components.InputField;
import com.lechucksoftware.proxy.proxysettings.ui.fragments.base.BaseDialogFragment;
import com.lechucksoftware.proxy.proxysettings.utils.EventReportingUtils;
import com.lechucksoftware.proxy.proxysettings.utils.NavigationUtils;
import com.lechucksoftware.proxy.proxysettings.utils.UIUtils;
import com.shouldit.proxy.lib.ProxyStatusItem;
import com.shouldit.proxy.lib.enums.ProxyStatusProperties;
import com.shouldit.proxy.lib.utils.ProxyUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class ProxyDetailFragment extends BaseDialogFragment
{
    public static ProxyDetailFragment instance;
    public static final String TAG = ProxyDetailFragment.class.getSimpleName();

    // Arguments
    private static final String SELECTED_PROXY_ARG = "SELECTED_PROXY_ARG";

    private InputField proxyHost;
    private InputField proxyPort;
    private InputExclusionList proxyBypass;
//    private InputTags proxyTags;
    private ProxyEntity selectedProxy;
    private UUID cachedObjId;
    private UIHandler uiHandler;
    private RelativeLayout proxyInUseBanner;
    private RelativeLayout proxyDuplicatedBanner;

    private ScrollView proxyScrollView;
    private Map<ProxyStatusProperties,CharSequence> validationErrors;


    public static ProxyDetailFragment newInstance(UUID cachedObjId)
    {
        ProxyDetailFragment instance = new ProxyDetailFragment();

        Bundle args = new Bundle();
        args.putSerializable(SELECTED_PROXY_ARG, cachedObjId);
        instance.setArguments(args);

        return instance;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.proxy_preferences, container, false);

        getUIComponents(v);
        return v;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        Bundle args = getArguments();
        uiHandler = new UIHandler();
        validationErrors = new HashMap<ProxyStatusProperties, CharSequence>();

        if (args != null && args.containsKey(SELECTED_PROXY_ARG))
        {
            cachedObjId = (UUID) getArguments().getSerializable(SELECTED_PROXY_ARG);
            selectedProxy = (ProxyEntity) App.getCacheManager().get(cachedObjId);
        }

        if (selectedProxy == null)
        {
            // TODO: temporary fix. needs improvements. remove cachemanager!
            NavigationUtils.GoToMainActivity(getActivity());
        }

        uiHandler.callRefreshUI();
    }

    private void getUIComponents(View v)
    {
        proxyScrollView = (ScrollView) v.findViewById(R.id.proxy_scrollview);
        proxyInUseBanner = (RelativeLayout) v.findViewById(R.id.proxy_in_use_banner);
        proxyDuplicatedBanner = (RelativeLayout) v.findViewById(R.id.proxy_duplicated_banner);

        proxyHost = (InputField) v.findViewById(R.id.proxy_host);
        proxyHost.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {  }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) { }

            @Override
            public void afterTextChanged(Editable editable)
            {
                checkValidation();
            }
        });

        proxyPort = (InputField) v.findViewById(R.id.proxy_port);
        proxyPort.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3)
            {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3)
            {

            }

            @Override
            public void afterTextChanged(Editable editable)
            {
                checkValidation();
            }
        });

        proxyBypass = (InputExclusionList) v.findViewById(R.id.proxy_bypass);
        proxyBypass.addValueChangedListener(new InputExclusionList.ValueChangedListener()
        {
            @Override
            public void onExclusionListChanged(String result)
            {

                // TODO: Improve scrolling to focused bypass item -> Temporary disabled
//                proxyScrollView.scrollTo(0,proxyScrollView.getBottom());

                checkValidation();
            }
        });

//        proxyTags = (InputTags) v.findViewById(R.id.proxy_tags);
//        proxyTags.setTagsViewOnClickListener(new View.OnClickListener()
//        {
//            @Override
//            public void onClick(View view)
//            {
//                TagsListFragment tagsListSelectorFragment = TagsListFragment.newInstance(cachedObjId);
//                tagsListSelectorFragment.show(getFragmentManager(), TAG);
//            }
//        });
    }

    private boolean validateBypass()
    {
        String value = proxyBypass.getExclusionString();
        App.getLogger().d(TAG, "Exclusion list updated: " + value);

        ProxyStatusItem item = ProxyUtils.isProxyValidExclusionList(value);
        validationErrors.remove(item.statusCode);
        if (!item.result)
        {
            validationErrors.put(item.statusCode,item.message);
            return false;
        }
        else
        {
            selectedProxy.exclusion = value;
            return true;
        }
    }

    private boolean validateHost()
    {
        String value = proxyHost.getValue();

        proxyHost.setError(null);
        ProxyStatusItem item = ProxyUtils.isProxyValidHostname(value);
        validationErrors.remove(item.statusCode);

        if (!item.result)
        {
            proxyHost.setError(item.message);
            validationErrors.put(item.statusCode, item.message);
            return false;
        }
        else
        {
            selectedProxy.host = value;
            return true;
        }
    }

    private boolean validatePort()
    {
        Integer value = null;

        try
        {
            value = Integer.parseInt(proxyPort.getValue());
        }
        catch (NumberFormatException e)
        {
            value = Integer.MAX_VALUE;
        }

        ProxyStatusItem item = ProxyUtils.isProxyValidPort(value);
        validationErrors.remove(item.statusCode);

        proxyPort.setError(null);
        if (!item.result)
        {
            proxyPort.setError(item.message);
            validationErrors.put(item.statusCode, item.message);
            return false;
        }
        else
        {
            selectedProxy.port = value;
            return true;
        }
    }

    private void checkValidation()
    {
        if (
            validateHost() &&
            validatePort() &&
            validateBypass())
        {
            ((ProxyDetailActivity)getActivity()).enableSave();
        }
        else
        {
            ((ProxyDetailActivity)getActivity()).disableSave();
        }


        // TODO: Add check for duplicated configuration to Async handler
        proxyDuplicatedBanner.setVisibility(View.GONE);
        String host = selectedProxy.host;
        Integer port = selectedProxy.port;
        if (host != null && port != null)
        {
            List<Long> duplicatedIDs = App.getDBManager().findDuplicatedProxy(host, port);
            if (selectedProxy.isPersisted)
            {
                proxyDuplicatedBanner.setVisibility(UIUtils.booleanToVisibility(duplicatedIDs.size() > 1));
            }
            else
            {
                proxyDuplicatedBanner.setVisibility(UIUtils.booleanToVisibility(duplicatedIDs.size() > 0));
            }
        }
    }

    private void refreshUI()
    {
        if (selectedProxy != null)
        {
            proxyInUseBanner.setVisibility(UIUtils.booleanToVisibility(selectedProxy.getInUse()));

            proxyHost.setValue(selectedProxy.host);
            if (selectedProxy.port != null && selectedProxy.port != 0)
            {
                proxyPort.setValue(selectedProxy.port);
            }

            proxyBypass.setExclusionString(selectedProxy.exclusion);
//                proxyTags.setTags(selectedProxy.getTags());

            checkValidation();
        }
        else
        {
            // TODO: Add handling here
            EventReportingUtils.sendException(new Exception("NO PROXY SELECTED"));
        }
    }

    private class UIHandler extends Handler
    {
        @Override
        public void handleMessage(Message message)
        {
            Bundle b = message.getData();

            App.getLogger().w(TAG, "handleMessage: " + b.toString());

            refreshUI();
        }

        public void callRefreshUI()
        {
            sendEmptyMessage(0);
        }
    }
}