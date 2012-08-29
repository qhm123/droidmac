//
//  Document.h
//  droidmac
//
//  Created by qhm123 on 12-8-24.
//  Copyright (c) 2012年 qhm123. All rights reserved.
//

#import <Cocoa/Cocoa.h>


@interface AppInfo : NSObject
@property (strong) NSString *package;
@property (strong) NSString *launch;
@end

@interface Document : NSDocument
@property (weak) IBOutlet NSProgressIndicator *progress;
@property (weak) IBOutlet NSTextField *text;
@property (weak) IBOutlet NSImageView *icon;
@property (weak) IBOutlet NSTextField *appName;
@property (weak) IBOutlet NSTextField *appVersion;
@property (weak) IBOutlet NSButton *cancelBtn;
- (IBAction)cancel:(id)sender;

@property (strong) AppInfo *curAppInfo;
@property (strong) NSTask *instTask;

@end

