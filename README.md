# vendor_pixel-framework

## Warning

1. This is not a guide or automation script.
2. This is very dirty hacky stuffs for achieve impl.
3. This is only for references so that we can not forgot the sequence...

## TODO: Find a much better and cleaner way to impl sequences and automate it

```bash
# 1. Dump firmware
# 2. Build AOSP without vendor frameworks impls for comparing and figure out which part is needed
# 3. Install frameworks-res.apk and vendor modified SystemUI.apk for works
apktool if framework-res.apk
apktool if SystemUIGoogle.apk

# 4. Decompile vendor modified SystemUI.apk and Settings.apk without decompiling resources
#    Since APKTool's update, there's no need to replace resource hexes with R.java
#    For preparing sources
apktool d -r SystemUIGoogle.apk
apktool d -r SettingsGoogle.apk
#    For preparing resources (Full decompile)
apktool d SystemUIGoogle.apk
apktool d SettingsGoogle.apk

# 5. Redirect all other R.java to main R.java as we included
#    decompiled resources directly instead of creating aar library
#    For example, in vendor modified Settings.apk
for i in $(grep -rl 'com/google/android/material/R\$' | grep 'smali')
do 
    if [[ $i != *"R\$"* ]]; then
        echo $i
        sed -i -e 's/com\/google\/android\/material\/R\$/com\/android\/settings\/R\$/g' $i
    fi
done

for i in $(grep -rl 'com/google/android/settings/R\$' | grep 'smali')
do
    if [[ $i != *"R\$"* ]]; then
        echo $i
        sed -i -e 's/com\/google\/android\/settings\/R\$/com\/android\/settings\/R\$/g' $i
    fi
done

for i in $(grep -rl 'com/google/android/wifitrackerlib/R\$' | grep 'smali')
do
    if [[ $i != *"R\$"* ]]; then
        echo $i
        sed -i -e 's/com\/google\/android\/wifitrackerlib\/R\$/com\/android\/settings\/R\$/g' $i
    fi
done

for i in $(grep -rl 'com/google/android/setupcompat/R\$' | grep 'smali')
do
    if [[ $i != *"R\$"* ]]; then
        echo $i
        sed -i -e 's/com\/google\/android\/setupcompat\/R\$/com\/android\/settings\/R\$/g' $i
    fi
done

for i in $(grep -rl 'com/google/android/setupdesign/R\$' | grep 'smali')
do
    if [[ $i != *"R\$"* ]]; then
        echo $i
        sed -i -e 's/com\/google\/android\/setupdesign\/R\$/com\/android\/settings\/R\$/g' $i
    fi
done

# 6. Replace "-" with "_" in source files' name
for i in $(find ./ -name '\-IA' | grep 'smali')
do
    mv $i ${i/\-IA/_IA}
done

for i in $(grep -rl '\-IA' | grep 'smali')
do
    sed -i 's/\-IA/_IA/g' $i
done

# 7. Compile modified apks with apktool
apktool b SystemUIGoogle
apktool b SettingsGoogle

# 8. Fix and import/update resources that not duplicated with AOSP
#    This is seriously dirtiest part as I'm really bad at writing scripts...
# WARN: DO NOT RUN THIS DIRECTLY. IT WILL MORE BROKEN THAN FIXING.
for i in $(grep -rl '?android:^attr-private')
do
    echo $i
    sed -i 's/?android:^attr-private/?androidprv:attr/g' $i
done

folder=vendor/pixel-framework/SettingsGoogle
for file in packages/apps/Settings/res/values/*.xml;
do
    for i in $(find $folder/res/values -name *xml ! -path "$folder/res/raw" ! -path "$folder/res/drawable*" ! -path "$folder/res/xml")
    do
        awk 'FNR==NR{lines[$0]=1;next} $0 in lines' $file $i >>z_pixel-fw-works/$(basename $i)
    done
done

cat z_pixel-fw-works/*.xml > z_pixel-fw-works/exclude-tag.txt
rm z_pixel-fw-works/*.xml

for file in packages/apps/Settings/res-product/values/*.xml;
do
    for i in $(find $folder/res/values -name *xml ! -path "$folder/res/raw" ! -path "$folder/res/drawable*" ! -path "$folder/res/xml")
    do
        awk 'FNR==NR{lines[$0]=1;next} $0 in lines' $file $i >>z_pixel-fw-works/$(basename $i)
    done
done

cat z_pixel-fw-works/*.xml > z_pixel-fw-works/exclude-tag.txt
rm z_pixel-fw-works/*.xml

# 9. Sort z_pixel-fw-works/exclude-tag.txt manually by text editor
#    You need to make all tags into somewhat like
#    array:allowlist_hide_summary_in_battery_usage
#    attr:actionTextColorAlpha
#    style:WorkChallengeEmergencyButtonStyle
#    etc.
#    After that, run uniq
uniq z_pixel-fw-works/exclude-tag.txt > z_pixel-fw-works/exclude-tmp.txt
mv z_pixel-fw-works/exclude-tmp.txt z_pixel-fw-works/exclude-tag.txt

# 10. This part is taken from extract-files.sh scripts in GMS
# AGAIN, DO NOT RUN THIS DIRECTLY. IT WILL MORE BROKEN THAN FIXING.
folder=vendor/pixel-framework/SettingsGoogle
for file in $(find $folder/res -name strings.xml ! -path "$folder/res/raw" ! -path "$folder/res/drawable*" ! -path "$folder/res/xml"); do
    for tag in $(cat z_pixel-fw-works/exclude-tag.txt); do
        type=$(echo $tag | cut -d: -f1)
        node=$(echo $tag | cut -d: -f2)
        xmlstarlet ed -L -d "/resources/$type[@name="\'$node\'"]" $file
        xmlstarlet fo -s 4 $file > $file.bak
        mv $file.bak $file
    done
    sed -i "s|\?android:\^attr-private|\@\*android\:attr|g" $file
    sed -i "s|\@android\:color|\@\*android\:color|g" $file
    sed -i "s|\^attr-private|attr|g" $file
done

# 11. Open vendor modified apks' decomplied folder that includes AndroidManifest.xml in Android Studio
# Click AndroidManifest.xml
# ALT+Shift+L
# Select `Rearrange entries`
# And `OK`
# Android Studio will formatting messed AndroidManifest.xml to human readable form...

# 11. Open vendor/pixel-framework/SettingsGoogle in Android Studio
# Click res folder and AndroidManifest.xml
# ALT+Shift+L
# Select `Rearrange entries`
# And `OK`
# Android Studio will formatting messed xmls to human readable form...

# 12. Manually check resources and fix...

# 13. Extract class files from apk that we worked (dexs)
d2j-dex2jar.sh SettingsGoogle.apk

# 14. Unzip
unzip SettingsGoogle-dex2jar.jar

# 15. Remove all parts that don't required and keep that we want to impl

# 16. Zip it into jar
zip -r SettingsGoogle-lib.jar ./

# 17. Check if those stuffs are buildable and working as expected
#     If we need to modify sources, use jadx to decompile and remove class with same name as java
#     And save it into src folder
#     --show-bad-code option will enforce showing uncompilable code instead of non-decompiled
#     so that we can use those as a reference
jadx --show-bad-code SettingsGoogle-lib.jar
#     Or editing smali directly...

# 18. ???
# 19. Profit

```
