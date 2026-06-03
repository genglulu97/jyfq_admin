package com.jyfq.loan.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * H5 application form submitted by the promotion page.
 */
@Data
public class H5ApplyRequestDTO implements Serializable {

    @NotBlank(message = "channelCode is required")
    private String channelCode;

    @NotBlank(message = "age is required")
    private String age;

    @JsonAlias({"profession"})
    @NotBlank(message = "occupation is required")
    private String occupation;

    @JsonAlias({"workCity"})
    @NotBlank(message = "city is required")
    private String city;

    @JsonAlias({"socialSecurity"})
    @NotBlank(message = "socialSec is required")
    private String socialSec;

    @JsonAlias({"householdRegister"})
    @NotBlank(message = "hukou is required")
    private String hukou;

    @JsonAlias({"householdCity"})
    private String hukouCity;

    @NotBlank(message = "zhima is required")
    private String zhima;

    @JsonAlias({"providentFund"})
    @NotBlank(message = "gongjijin is required")
    private String gongjijin;

    @NotBlank(message = "socialSecSupply is required")
    private String socialSecSupply;

    @NotBlank(message = "carHouse is required")
    private String carHouse;

    private List<String> assets;

    @NotBlank(message = "phone is required")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "phone format is invalid")
    private String phone;

    private String verifyCode;

    @JsonAlias({"loanAmount"})
    private String amount;

    private Integer loanTime;

    private String deviceIp;
}
