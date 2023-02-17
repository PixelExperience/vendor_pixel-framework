/*
 * Copyright (C) 2023 The PixelExperience Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.systemui.smartspace;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.android.systemui.R;
import com.android.systemui.smartspace.nano.SmartspaceProto;

public final class SmartSpaceCard {
    public static int sRequestCode;
    public final SmartspaceProto.SmartspaceUpdate.SmartspaceCard mCard;
    public final Context mContext;
    public Bitmap mIcon;
    public boolean mIconProcessed;
    public final Intent mIntent;
    public final long mPublishTime;
    public int mRequestCode;

    public SmartSpaceCard(Context context, SmartspaceProto.SmartspaceUpdate.SmartspaceCard smartspaceCard, Intent intent, Bitmap bitmap, long j) {
        this.mContext = context.getApplicationContext();
        this.mCard = smartspaceCard;
        this.mIntent = intent;
        this.mIcon = bitmap;
        this.mPublishTime = j;
        int i = sRequestCode + 1;
        sRequestCode = i;
        if (i > 2147483646) {
            sRequestCode = 0;
        }
        this.mRequestCode = sRequestCode;
    }

    public boolean isSensitive() {
        return this.mCard.isSensitive;
    }

    public boolean isWorkProfile() {
        return this.mCard.isWorkProfile;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > getExpiration();
    }

    public Intent getIntent() {
        return this.mIntent;
    }

    public Bitmap getIcon() {
        return this.mIcon;
    }

    public void setIcon(Bitmap bitmap) {
        this.mIcon = bitmap;
    }

    public void setIconProcessed(boolean z) {
        this.mIconProcessed = z;
    }

    public boolean isIconProcessed() {
        return this.mIconProcessed;
    }

    public String getTitle() {
        return substitute(true);
    }

    public String getSubTitle() {
        return substitute(false);
    }

    public static SmartSpaceCard fromWrapper(Context context, SmartspaceProto.CardWrapper cardWrapper, boolean z) {
        Intent intent;
        Bitmap cardIcon;
        try {
            SmartspaceProto.SmartspaceUpdate.SmartspaceCard.TapAction tapAction = cardWrapper.card.tapAction;
            if (tapAction != null && !TextUtils.isEmpty(tapAction.intent)) {
                intent = Intent.parseUri(cardWrapper.card.tapAction.intent, 0);
            } else {
                intent = null;
            }
            byte[] cardIconFromWrapper = cardWrapper.icon;
            if (cardIconFromWrapper != null) {
                cardIcon = BitmapFactory.decodeByteArray(cardIconFromWrapper, 0, cardIconFromWrapper.length, null);
            } else {
                cardIcon = null;
            }
            int dimensionPixelSize = context.getResources().getDimensionPixelSize(R.dimen.header_icon_size);
            if (cardIcon != null && cardIcon.getHeight() > dimensionPixelSize) {
                cardIcon = Bitmap.createScaledBitmap(cardIcon, (dimensionPixelSize / cardIcon.getHeight()) * cardIcon.getWidth(), dimensionPixelSize, true);
            }
            return new SmartSpaceCard(context, cardWrapper.card, intent, cardIcon, cardWrapper.publishTime);
        } catch (Exception e) {
            Log.e("SmartspaceCard", "from proto", e);
            return null;
        }
    }

    public String getDurationText(SmartspaceProto.SmartspaceUpdate.SmartspaceCard.Message.FormattedText.FormatParam formatParam) {
        long j;
        if (formatParam.formatParamArgs == 2) {
            SmartspaceProto.SmartspaceUpdate.SmartspaceCard smartspaceCard = this.mCard;
            j = smartspaceCard.eventTimeMillis + smartspaceCard.eventDurationMillis;
        } else {
            j = this.mCard.eventTimeMillis;
        }
        int ceil = (int) Math.ceil(Math.abs(System.currentTimeMillis() - j) / 60000.0d);
        if (ceil >= 60) {
            int i = ceil / 60;
            int i2 = ceil % 60;
            String quantityString = this.mContext.getResources().getQuantityString(R.plurals.smartspace_hours, i, Integer.valueOf(i));
            return i2 > 0 ? this.mContext.getString(R.string.smartspace_hours_mins, quantityString, this.mContext.getResources().getQuantityString(R.plurals.smartspace_minutes, i2, Integer.valueOf(i2))) : quantityString;
        }
        return this.mContext.getResources().getQuantityString(R.plurals.smartspace_minutes, ceil, Integer.valueOf(ceil));
    }

    public long getExpiration() {
        SmartspaceProto.SmartspaceUpdate.SmartspaceCard.ExpiryCriteria expiryCriteria;
        SmartspaceProto.SmartspaceUpdate.SmartspaceCard smartspaceCard = this.mCard;
        if (smartspaceCard != null && (expiryCriteria = smartspaceCard.expiryCriteria) != null) {
            return expiryCriteria.expirationTimeMillis;
        }
        return 0L;
    }

    public PendingIntent getPendingIntent() {
        if (this.mCard.tapAction == null) {
            return null;
        }
        Intent intent = new Intent(this.mIntent);
        int i = this.mCard.tapAction.actionType;
        if (i != 1) {
            if (i != 2) {
                return null;
            }
            return PendingIntent.getActivity(this.mContext, this.mRequestCode, intent, 67108864);
        }
        intent.addFlags(268435456);
        intent.setPackage("com.google.android.googlequicksearchbox");
        return PendingIntent.getBroadcast(this.mContext, this.mRequestCode, intent, 0);
    }

    public String getFormattedTitle() {
        SmartspaceProto.SmartspaceUpdate.SmartspaceCard.Message.FormattedText formattedText;
        String str;
        SmartspaceProto.SmartspaceUpdate.SmartspaceCard.Message.FormattedText.FormatParam[] formatParamArr;
        SmartspaceProto.SmartspaceUpdate.SmartspaceCard.Message message = getMessage();
        if (message == null || (formattedText = message.title) == null || (str = formattedText.text) == null) {
            return "";
        }
        if (!hasParams(formattedText)) {
            return str;
        }
        String str2 = null;
        String str3 = null;
        int i = 0;
        while (true) {
            formatParamArr = formattedText.formatParam;
            if (i >= formatParamArr.length) {
                break;
            }
            SmartspaceProto.SmartspaceUpdate.SmartspaceCard.Message.FormattedText.FormatParam formatParam = formatParamArr[i];
            if (formatParam != null) {
                int i2 = formatParam.formatParamArgs;
                if (i2 == 1 || i2 == 2) {
                    str3 = getDurationText(formatParam);
                } else if (i2 == 3) {
                    str2 = formatParam.text;
                }
            }
            i++;
        }
        SmartspaceProto.SmartspaceUpdate.SmartspaceCard smartspaceCard = this.mCard;
        if (smartspaceCard.cardType == 3 && formatParamArr.length == 2) {
            str3 = formatParamArr[0].text;
            str2 = formatParamArr[1].text;
        }
        if (str2 == null) {
            return "";
        }
        if (str3 == null) {
            if (message != smartspaceCard.duringEvent) {
                return str;
            }
            str3 = this.mContext.getString(R.string.smartspace_now);
        }
        return this.mContext.getString(R.string.smartspace_pill_text_format, str3, str2);
    }

    private boolean hasParams(SmartspaceProto.SmartspaceUpdate.SmartspaceCard.Message.FormattedText formattedText) {
        SmartspaceProto.SmartspaceUpdate.SmartspaceCard.Message.FormattedText.FormatParam[] formatParamArr;
        return (formattedText == null || formattedText.text == null || (formatParamArr = formattedText.formatParam) == null || formatParamArr.length <= 0) ? false : true;
    }

    public SmartspaceProto.SmartspaceUpdate.SmartspaceCard.Message getMessage() {
        SmartspaceProto.SmartspaceUpdate.SmartspaceCard.Message message;
        SmartspaceProto.SmartspaceUpdate.SmartspaceCard.Message message2;
        long currentTimeMillis = System.currentTimeMillis();
        SmartspaceProto.SmartspaceUpdate.SmartspaceCard smartspaceCard = this.mCard;
        long j = smartspaceCard.eventTimeMillis;
        long j2 = smartspaceCard.eventDurationMillis + j;
        if (currentTimeMillis < j && (message2 = smartspaceCard.preEvent) != null) {
            return message2;
        }
        if (currentTimeMillis > j2 && (message = smartspaceCard.postEvent) != null) {
            return message;
        }
        return smartspaceCard.duringEvent;
    }

    public String substitute(boolean isTitle) {
        SmartspaceProto.SmartspaceUpdate.SmartspaceCard.Message.FormattedText formattedText;
        String str;
        SmartspaceProto.SmartspaceUpdate.SmartspaceCard.Message message = getMessage();
        if (message != null) {
            if (isTitle) {
                formattedText = message.title;
            } else {
                formattedText = message.subtitle;
            }
        } else {
            formattedText = null;
        }
        if (formattedText == null || (str = formattedText.text) == null) {
            return "";
        }
        SmartspaceProto.SmartspaceUpdate.SmartspaceCard.Message.FormattedText.FormatParam[] formatParamArr = formattedText.formatParam;
        if (formatParamArr != null && formatParamArr.length > 0) {
            SmartspaceProto.SmartspaceUpdate.SmartspaceCard.Message.FormattedText.FormatParam[] formatParamArr2 = formattedText.formatParam;
            int length = formatParamArr.length;
            String[] strArr = new String[length];
            for (int i = 0; i < length; i++) {
                SmartspaceProto.SmartspaceUpdate.SmartspaceCard.Message.FormattedText.FormatParam formatParam = formatParamArr[i];
                int i2 = formatParam.formatParamArgs;
                if (i2 != 1 && i2 != 2) {
                    if (i2 != 3) {
                        strArr[i] = "";
                    } else {
                        String str2 = formatParam.text;
                        if (str2 == null) {
                            str2 = "";
                        }
                        strArr[i] = str2;
                    }
                } else {
                    strArr[i] = getDurationText(formatParam);
                }
            }
            return String.format(str, strArr);
        }
        return str;
    }

    @NonNull
    public String toString() {
        return "title:" + substitute(true) + " subtitle:" + substitute(false) + " expires:" + getExpiration() + " published:" + this.mPublishTime;
    }
}