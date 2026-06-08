项目介绍：
1，项目封装了BaseActivity和BaseFragment，通过findViewById集中管理控件初始化。

2，创建模块针对YouTube与日历事件设计了专属解析Model，确保数据符合标准协议格式。

3，历史记录模块中，数据库采用 SQLite(通过 HistoryDBManagerHelper)，实现单数据库双表设计（scanTable 和 createTable）；
                 同时封装了数据库管理方法，包括查询全部数据、单条记录查询和批量删除。

4，滑动控件展示上，主页面采用TabLayout+ViewPager2架构，根据点击位置position的不同来动态注入数据库表名，实现了对‘扫描’或‘创建’表单数据的展示。

5，关于编辑状态的改变，定义了Normal(普通)与Edit(编辑)两种PageMode状态，以此来实现普通模式与编辑模式的切换

6，图标点击效果上，历史记录页面单击列表项进入详情页面，长按进入编辑模式；编辑模式下支持全选及多项批量删除功能。

7，扫码模块引用了第三方ZXing Android Embedded组件库，利用其提供的DecoratedBarcodeView复合控件。自动处理相机流预览、扫描框绘制及扫描逻辑，简化功能开发。

8，支持实时相机扫码和相册选取图片解析，能够自动识别并分类处理YouTube链接，日历事件和普通文本类型。
