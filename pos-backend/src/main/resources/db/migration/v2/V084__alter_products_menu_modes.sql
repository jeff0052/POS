-- Phase1 自助餐: 商品加菜单模式过滤
ALTER TABLE products
  ADD COLUMN menu_modes JSON NULL
    COMMENT '可见模式["A_LA_CARTE","BUFFET","DELIVERY"]，NULL=全模式可见'
    AFTER image_id;
