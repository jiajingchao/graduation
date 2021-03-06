package com.jjc.qiqiharuniversity.biz.home.news;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;

import com.jjc.qiqiharuniversity.R;
import com.jjc.qiqiharuniversity.biz.home.BannerModel;
import com.jjc.qiqiharuniversity.common.EventBusEvents;
import com.jjc.qiqiharuniversity.common.EventBusManager;
import com.jjc.qiqiharuniversity.common.LogHelper;
import com.jjc.qiqiharuniversity.common.ObjectHelper;
import com.jjc.qiqiharuniversity.common.ToastManager;
import com.jjc.qiqiharuniversity.common.base.BaseFragment;
import com.jjc.qiqiharuniversity.http.BizRequest;
import com.jjc.qiqiharuniversity.http.RequestListener;
import com.jjc.qiqiharuniversity.common.util.BannerLoader;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;
import com.youth.banner.Banner;
import com.youth.banner.BannerConfig;

import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;

/**
 * Author jiajingchao
 * Created on 2021/1/4
 * Description:新闻快讯模块
 */

public class NewsFragment extends BaseFragment {

    private static final String TAG = NewsFragment.class.getSimpleName();
    private RecyclerView rvNews;
    private NewsItemListAdapter newsItemListAdapter;
    private ListNewsVO listNewsVO;
    private ListNewsResVO resVO;

    private Banner banner;
    // 轮播图列表
    private List<Integer> imgList = Arrays.asList(R.drawable.banner_img1, R.drawable.banner_img2, R.drawable.banner_img3);
    private List<String> titleList = Arrays.asList("2021牛年大吉", "众志成城，战胜疫情", "齐齐哈尔大学冬日摄影");
    private List<String> bmobImageUrlList = new ArrayList<>();
    private List<String> bmobImageTitleList = new ArrayList<>();

    private RefreshLayout news_refresh;

    @Override
    public int getRootLayout() {
        return R.layout.fragment_news;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBusManager.register(this);
    }

    @Override
    public void initView(@NonNull View view, @Nullable Bundle savedInstanceState) {
        banner = view.findViewById(R.id.banner);
        // 设置图片加载器
        banner.setImageLoader(new BannerLoader());
        // 显示图片
        banner.setBannerStyle(BannerConfig.CIRCLE_INDICATOR_TITLE_INSIDE);//设置页码与标题
        initBanner();

        rvNews = view.findViewById(R.id.rv_news);
        rvNews.setLayoutManager(new LinearLayoutManager(view.getContext()));

        resVO = new ListNewsResVO();
        resVO.type = "top";
        // 今日头条api的key
        resVO.key = getResources().getString(R.string.my_toutiao_key);
        getNewsList(resVO);

        news_refresh = view.findViewById(R.id.news_refresh);
        // 是否在刷新的时候禁止列表的操作
        news_refresh.setDisableContentWhenRefresh(false);
    }

    public void initBanner() {
        BmobQuery<BannerModel> query = new BmobQuery<>();
        query.addWhereEqualTo("isShow", Boolean.TRUE);
        query.order("ImageIndex");
        query.findObjects(new FindListener<BannerModel>() {
            @Override
            public void done(List<BannerModel> list, BmobException e) {
                if (e == null) {
                    for (BannerModel model : list) {
                        if (!ObjectHelper.isIllegal(model)) {
                            bmobImageUrlList.add(model.getImageUrl());
                            bmobImageTitleList.add(model.getImageTitle());
                        }
                    }
                    banner.setImages(bmobImageUrlList);
                    banner.setBannerTitles(bmobImageTitleList);
                    banner.setDelayTime(3000);
                    banner.setIndicatorGravity(BannerConfig.CENTER);
                    banner.start();
                    ToastManager.show(getContext(), "图片加载成功");
                } else {
                    LogHelper.i(TAG, e.getMessage());
                    banner.setImages(imgList);
                    banner.setBannerTitles(titleList);
                    banner.setDelayTime(3000);
                    banner.setIndicatorGravity(BannerConfig.CENTER);
                    banner.start();
                    ToastManager.show(getContext(), "图片加载失败");
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBusManager.unregister(this);
    }

    protected void initListener() {
        if (newsItemListAdapter != null) {
            newsItemListAdapter.setListener((view, position) -> {
                String detailsUrl = listNewsVO.getResult().getData().get(position).getUrl();
                NewsDetailsWebViewActivity.start(getContext(), detailsUrl);
            });
        }

        // 下拉刷新
        news_refresh.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(RefreshLayout refreshlayout) {
                listNewsVO = null;
                getNewsList(resVO);
            }
        });

    }

    /**
     * 访问头条接口获取新闻列表
     * @param resVO
     */
    private void getNewsList(ListNewsResVO resVO) {
        BizRequest.getInstance().getNewsList(resVO.type, resVO.key, new RequestListener<ListNewsVO>() {
            @Override
            public void onResponse(ListNewsVO listNewsVO) {
                if (ObjectHelper.isIllegal(listNewsVO)) {
                    ToastManager.show(getContext(), "获取失败，请检查网络或服务器出错");
                    news_refresh.finishRefresh(false);
                    return;
                }
                news_refresh.finishRefresh(true);
                LogHelper.i(TAG, "getNewsList Success");
                EventBusEvents.GetNewsListSuccessEvent event = new EventBusEvents.GetNewsListSuccessEvent();
                event.listNewsVO = listNewsVO;
                EventBusManager.post(event);
            }

            @Override
            public void onFailure(Throwable t) {
                LogHelper.i(TAG, "getNewsList Failed");
                t.printStackTrace();
            }
        });
    }

    @Subscribe
    public void onEvent(EventBusEvents.GetNewsListSuccessEvent event) {
        listNewsVO = event.listNewsVO;
        newsItemListAdapter = new NewsItemListAdapter(getContext(), listNewsVO);
        initListener();
        rvNews.setAdapter(newsItemListAdapter);
    }

}