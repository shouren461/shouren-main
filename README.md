一，项目介绍：
1，项目封装了BaseActivity和BaseFragment，通过findViewById集中管理控件初始化。

2，创建模块针对YouTube与日历事件设计了专属解析Model，确保数据符合标准协议格式。

3，历史记录模块中，数据库采用 SQLite(通过 HistoryDBManagerHelper)，实现单数据库双表设计（scanTable 和 createTable）；
                 同时封装了数据库管理方法，包括查询全部数据、单条记录查询和批量删除。

4，滑动控件展示上，主页面采用TabLayout+ViewPager2架构，根据点击位置position的不同来动态注入数据库表名，实现了对‘扫描’或‘创建’表单数据的展示。

5，关于编辑状态的改变，定义了Normal(普通)与Edit(编辑)两种PageMode状态，以此来实现普通模式与编辑模式的切换

6，图标点击效果上，历史记录页面单击列表项进入详情页面，长按进入编辑模式；编辑模式下支持全选及多项批量删除功能。

7，使用CameraX+Ml kit来实现项目中扫描功能

8，支持实时相机扫码，相册选取图片解析，拍照保存到相册，闪光灯开关，双指缩放，单击对焦，双击恢复相机原比例等功能，能够自动识别并分类处理YouTube链接，日历事件和普通文本类型。

以下是关于项目中的重点知识点总结和bug修复操作:

二,项目涉及的重点布局项总结：

1,android:scaleType = "center" ->按图片原始大小，居中展示

2,android:scaleType="centerInside" ->完整居中展示，但绝不拉伸

3,android:gravity = "center" ->文字居中

4,android:fontFamily = "sans-serif-medium" 使用安卓自带的中黑体字体

5,android:alpha = "0.2"  ->"阿尔法通道"，代表不透明度，不透明度设置为20%，即透明度为80%

6,app:tabIndicatorColor="@color/colorPrimary" ->代表所选中的指示器横条颜色

7,app:layout_constraintVertical_bias = "0.3" ->代表相机扫描控件在垂直方向上偏移量百分比，上层约占30%,下层约占70%

8,android:ellipsize = "end",如果文本显示不下，就在末尾显示省略号

9,android:scaleType = "centerCrop" ->等比例拉伸铺满，并居中裁剪

10,app:cardCornerRadius = "16dp" ->设置卡片圆角半径为16dp

11,app:cardElevation = "6dp"  ->卡片的阴影深度设置为6dp

三,bug修复操作:

1，window.decorView.post{
  initHistoryItemList()
}  ->作用是确保页面布局控件在完成初始化之后，再执行post的调用代码，避免因视图未完全加载完成而导致的各种bug（比如某些耗时操作如生成二维码，后台异步执行拍照和保存图片逻辑等）

2，修复了Youtube输入文本的提示符重叠bug
