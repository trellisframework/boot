package net.trellisframework.util.phone;

import net.trellisframework.util.text.TextUtil;
import net.trellisframework.core.log.Logger;
import net.trellisframework.core.AppInfo;
import net.trellisframework.core.constant.Country;
import com.google.i18n.phonenumbers.*;
import org.apache.commons.lang3.StringUtils;

public class PhoneUtil {

    public static Phone parse(String phone) {
        return parse(phone, AppInfo.getCountry());
    }

    public static Phone parse(String phone, Country defaultCountry) {
        phone = TextUtil.toEnglishNumber(phone);
        try {
            defaultCountry = defaultCountry == null ? AppInfo.getCountry() : defaultCountry;
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            Phonenumber.PhoneNumber pn = phoneUtil.parse(phone, defaultCountry.name());
            int countryCode = pn.getCountryCode();
            long nationalNumber = pn.getNationalNumber();
            String localNumber = toLocalNumber(phone, String.valueOf(pn.getCountryCode()));
            return new Phone(countryCode, nationalNumber, localNumber);
        } catch (NumberParseException e) {
            Logger.error("PhoneParseException", e.getMessage());
            return new Phone();
        }
    }

    public static String toHide(String phone) {
        if (phone == null)
            phone = "";
        if (phone.length() > 7)
            return "\u200E" + phone.substring(0, 7) + "****" + "\u200E";
        return phone;
    }

    public static boolean isValid(String phoneNumber) {
        return isValid(phoneNumber, AppInfo.getCountry());
    }

    public static boolean isValid(String phoneNumber, Country... countries) {
        try {
            if (countries == null || countries.length <= 0)
                countries = new Country[]{AppInfo.getCountry()};
            for (Country country : countries) {
                PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                Phonenumber.PhoneNumber pn = phoneUtil.parse(phoneNumber, country.name());
                if (phoneUtil.isValidNumberForRegion(pn, country.name()) || isValidLocalNumber(phoneNumber, String.valueOf(pn.getCountryCode())))
                    return true;
            }
            return false;
        } catch (NumberParseException e) {
            Logger.error("PhoneParseException", e.getMessage());
            return false;
        }
    }

    public static String toLocalNumber(String phone, String countryCode) {
        String res = StringUtils.isBlank(phone) ? StringUtils.EMPTY : phone.replace("+", "00").replaceAll("[^0-9]+", "");
        int subStringLength = res.startsWith("00" + countryCode) ? (countryCode.length() + 2) : res.startsWith(countryCode) ? countryCode.length() : 0;
        String remain = res.substring(subStringLength);
        return remain.startsWith("0") ? remain : "0" + remain;
    }

    static boolean isValidLocalNumber(String phone, String countryCode) {
        String plain = toLocalNumber(phone, countryCode);
        return plain.startsWith("0") && plain.length() == 11;
    }
}
