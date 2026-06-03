package com.jyfq.loan.service;

import com.jyfq.loan.model.vo.H5IpCityVO;

public interface H5LocationService {

    H5IpCityVO resolveIpCity(String ip);
}
