package com.sudd.doc.Document.Enum;
import static com.sudd.doc.Document.Constant.Constants.ADMIN_AUTHORITIES;
import static com.sudd.doc.Document.Constant.Constants.MANAGER_AUTHORITIES;
import static com.sudd.doc.Document.Constant.Constants.SUPER_ADMIN_AUTHORITIES;
import static  com.sudd.doc.Document.Constant.Constants.USER_AUTHORITIES;;

public enum Authority {

  USER(USER_AUTHORITIES),
  ADMIN(ADMIN_AUTHORITIES),
  SUPER_ADMIN(SUPER_ADMIN_AUTHORITIES),
  MANAGER(MANAGER_AUTHORITIES);


  private final String value;

  private Authority(String value) {
    this.value = value;
}

  public String getValue() {
    return this.value;
}
 
}
