package org.project.employeemanagementsystem.controller;

import org.project.employeemanagementsystem.util.Navigator;
import org.project.employeemanagementsystem.util.UserSession;
import org.springframework.beans.factory.annotation.Autowired;


public abstract class BaseController {

    @Autowired
    protected UserSession userSession;

    @Autowired
    protected Navigator navigator;

}