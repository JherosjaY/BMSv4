package com.example.blottermanagementsystem.data.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.blottermanagementsystem.data.dao.*;
import com.example.blottermanagementsystem.data.entity.*;

@Database(
    entities = {
        User.class, BlotterReport.class, Suspect.class, Witness.class, Evidence.class,
        Hearing.class, StatusHistory.class, Resolution.class, Officer.class, InvestigationTask.class,
        Notification.class, Status.class, Person.class, Respondent.class, PersonHistory.class,
        SmsNotification.class, RespondentStatement.class, Summons.class,
        MediationSession.class, CaseTimeline.class, CaseTemplate.class, SyncQueue.class,
        ConnectedDevice.class, LegalDocument.class, CloudinaryImage.class
    },
    version = 20,
    exportSchema = false
)
public abstract class BlotterDatabase extends RoomDatabase {
    
    public abstract UserDao userDao();
    public abstract BlotterReportDao blotterReportDao();
    public abstract SuspectDao suspectDao();
    public abstract WitnessDao witnessDao();
    public abstract EvidenceDao evidenceDao();
    public abstract HearingDao hearingDao();
    public abstract StatusHistoryDao statusHistoryDao();
    public abstract ResolutionDao resolutionDao();
    public abstract OfficerDao officerDao();
    public abstract NotificationDao notificationDao();
    public abstract StatusDao statusDao();
    public abstract PersonDao personDao();
    public abstract RespondentDao respondentDao();
    public abstract PersonHistoryDao personHistoryDao();
    public abstract SmsNotificationDao smsNotificationDao();
    public abstract RespondentStatementDao respondentStatementDao();
    public abstract SummonsDao summonsDao();
    public abstract MediationSessionDao mediationSessionDao();
    public abstract CaseTimelineDao caseTimelineDao();
    public abstract CaseTemplateDao caseTemplateDao();
    public abstract SyncQueueDao syncQueueDao();
    public abstract ConnectedDeviceDao connectedDeviceDao();
    public abstract LegalDocumentDao legalDocumentDao();
    public abstract InvestigationTaskDao investigationTaskDao();
    public abstract CloudinaryImageDao cloudinaryImageDao();
    
    private static volatile BlotterDatabase INSTANCE;
    
    public static BlotterDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (BlotterDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            BlotterDatabase.class, "blotter_database")
                            .fallbackToDestructiveMigration()
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(androidx.sqlite.db.SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    new Thread(() -> populateDatabase(context)).start();
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }
    
    private static void populateDatabase(Context context) {
        BlotterDatabase db = getDatabase(context);
        StatusDao statusDao = db.statusDao();
        
        statusDao.insertStatus(new Status("Pending"));
        statusDao.insertStatus(new Status("Under Investigation"));
        statusDao.insertStatus(new Status("For Mediation"));
        statusDao.insertStatus(new Status("Mediation Ongoing"));
        statusDao.insertStatus(new Status("Settled"));
        statusDao.insertStatus(new Status("Resolved"));
        statusDao.insertStatus(new Status("Closed"));
    }
}
