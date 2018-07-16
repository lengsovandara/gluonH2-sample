/**
 * Copyright (c) 2016, 2018 Gluon
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of Gluon, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL GLUON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gluonhq.h2;

import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.StorageService;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import com.gluonhq.h2.model.Person;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BasicView extends View {

    private final static String DB_NAME = "sample.db";

    private ListView<Person> listView;
    private final ListView<String> status;

    private Connection connection = null;
    private Statement stmt;
    private ResultSet rs;

    public BasicView() {

        try {
            Class c = Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Error class not found " + e);
        }

        listView = new ListView<>();
        listView.setPlaceholder(new Label("No items yet"));
        listView.setCellFactory(param -> {
            return new ListCell<Person>() {
                @Override
                protected void updateItem(Person item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null && !empty) {
                        setText(item.getFirstName() + " " + item.getLastName());
                    } else {
                        setText(null);
                    }
                }
            };
        });

        final Label labelDB = new Label("DB Items");
        labelDB.setStyle("-fx-text-fill: gray");
        VBox vListDB = new VBox(10, labelDB, listView);
        vListDB.setAlignment(Pos.CENTER_LEFT);

        status = new ListView<>();

        final Label labelStatus = new Label("Log");
        labelStatus.setStyle("-fx-text-fill: gray");
        VBox vListStatus = new VBox(10, labelStatus, status);
        vListStatus.setAlignment(Pos.CENTER_LEFT);

        VBox controls = new VBox(20, vListDB, vListStatus);
        controls.setPadding(new Insets(10));
        controls.setAlignment(Pos.CENTER);
        
        setCenter(controls);
    }

    @Override
    protected void updateAppBar(AppBar appBar) {
        appBar.setNavIcon(MaterialDesignIcon.PERSON_PIN.button());
        appBar.setTitleText("H2");
        appBar.getActionItems().addAll(
                MaterialDesignIcon.CREATE_NEW_FOLDER.button(e -> createDB()),
                MaterialDesignIcon.REMOVE.button(e -> {
                    listView.getItems().clear();
                    status.getItems().clear();
                }));
    }

    private void createDB() {
        
        status.getItems().add("Creating a Database with H2");
        File dir;
        String dbUrl = "jdbc:h2:";
        try {
            dir = Services.get(StorageService.class)
                    .map(s -> s.getPrivateStorage().get())
                    .orElseThrow(() -> new IOException("Error: PrivateStorage not available"));
            File db = new File (dir, DB_NAME);
            dbUrl = dbUrl + db.getAbsolutePath();
        } catch (IOException ex) {
            status.getItems().add("Error " + ex.getMessage());
            return;
        }

        try {
            connection = DriverManager.getConnection(dbUrl);
            status.getItems().add("Connection established: " + dbUrl);
        } catch (SQLException ex) {
            status.getItems().add("Error establishing connection " + ex.getSQLState());
            return;
        }

        List<Person> list = new ArrayList<>();

        try {
            if (connection != null) {
                stmt = connection.createStatement();
                stmt.setQueryTimeout(30);

                status.getItems().add("Creating table 'contact'...");
                String sql="DROP TABLE IF EXISTS contact;\n" +
                        "CREATE TABLE contact(\n" +
                        "        Id INT NOT NULL AUTO_INCREMENT,\n" +
                        "        Name VARCHAR(255) NOT NULL,\n" +
                        "        Email VARCHAR(255) NOT NULL UNIQUE,\n" +
                        "        PRIMARY KEY (ID)\n" +
                        ");";
                stmt.executeUpdate(sql);
                stmt.executeUpdate("INSERT INTO contact VALUES(1, 'Johan', 'Vos'), (2, 'Eugene', 'Ryzhikov')");

                status.getItems().add("Retrieving records from table 'contact'...");
                rs = stmt.executeQuery("select * from contact");
                while (rs.next()) {
                    String firstname = rs.getString("Name");
                    String lastname = rs.getString("Email");
                    list.add(new Person(firstname, lastname));
                }
                status.getItems().add("End creating table and retrieving records");
            }
        } catch (SQLException ex) {
            status.getItems().add("SQL error " + ex.getMessage());
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException ex) {
                status.getItems().add("SQL error " + ex.getSQLState());
            }
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException ex) {
                status.getItems().add("SQL error " + ex.getSQLState());
            }
        }

        listView.getItems().setAll(list);
    }

}
