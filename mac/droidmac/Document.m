//
//  Document.m
//  droidmac
//
//  Created by qhm123 on 12-8-24.
//  Copyright (c) 2012年 qhm123. All rights reserved.
//

#import "Document.h"

@implementation AppInfo
@synthesize package;
@synthesize launch;

@end

@implementation Document
@synthesize appName;
@synthesize appVersion;
@synthesize cancelBtn;
@synthesize progress;
@synthesize text;
@synthesize icon;

@synthesize curAppInfo;
@synthesize instTask;

- (id)init
{
    self = [super init];
    if (self) {
        // Add your subclass-specific initialization here.
    }
    return self;
}

- (NSString *)windowNibName
{
    // Override returning the nib file name of the document
    // If you need to use a subclass of NSWindowController or if your document supports multiple NSWindowControllers, you should remove this method and override -makeWindowControllers instead.
    return @"Document";
}

- (void)windowControllerDidLoadNib:(NSWindowController *)aController
{
    [super windowControllerDidLoadNib:aController];
    // Add any code here that needs to be executed once the windowController has loaded the document's window.
    
    [progress startAnimation:self];
}

+ (BOOL)autosavesInPlace
{
    return YES;
}

- (NSData *)dataOfType:(NSString *)typeName error:(NSError **)outError
{
    return nil;
}

- (BOOL)readFromData:(NSData *)data ofType:(NSString *)typeName error:(NSError **)outError
{
    return YES;
}

- (AppInfo *) getApkInfo:(NSString *)appPath {
    
    NSBundle *mainBundle=[NSBundle mainBundle];
    NSString *aaptPath=[mainBundle pathForResource:@"aapt" ofType:nil];
    
    NSTask *task;
    task = [[NSTask alloc] init];
    [task setLaunchPath:aaptPath];
    NSArray *arguments = [NSArray arrayWithObjects: @"dump", @"badging", appPath, nil];
    [task setArguments: arguments];
    
    NSPipe *pipe;
    pipe = [NSPipe pipe];
    [task setStandardOutput: pipe];
    
    NSFileHandle *file;
    file = [pipe fileHandleForReading];
    
    [task launch];
    
    NSData *data;
    data = [file readDataToEndOfFile];
    
    NSString *string;
    string = [[NSString alloc] initWithData: data
                                   encoding: NSUTF8StringEncoding];
    
    NSRegularExpression *appLabelRegx = [NSRegularExpression regularExpressionWithPattern:@"package: name='(.*?)'.*versionName='(.*?)'.*application-label:'(.*?)'.*application-icon.*:'(.*?)'.*launchable-activity: name='(.*?)'" options:NSRegularExpressionDotMatchesLineSeparators error:nil];
    NSTextCheckingResult *match = [appLabelRegx firstMatchInString:string options:0 range:NSMakeRange(0, [string length])];
    if(match != nil) {
        NSString *package = [string substringWithRange:[match  rangeAtIndex:1]];
        NSString *version = [string substringWithRange:[match  rangeAtIndex:2]];
        NSString *appLabel = [string substringWithRange:[match rangeAtIndex:3]];
        NSString *iconPath = [string substringWithRange:[match rangeAtIndex:4]];
        NSString *launch = [string substringWithRange:[match rangeAtIndex:5]];
        NSLog(@"package: %@, version: %@, applabel: %@, icon: %@, launch: %@. appPath: %@", package, version, appLabel, iconPath, launch, appPath);
        
        [self setAppIcon:appPath icon:iconPath];
        
        [appName setStringValue:appLabel];
        [appVersion setStringValue:version];
        
        AppInfo *appInfo = [AppInfo alloc];
        appInfo.launch = launch;
        appInfo.package = package;
        
        [self setCurAppInfo:appInfo];
        
        return appInfo;
    }
    
    return nil;
    
}

- (void) setAppIcon:(NSString *)appPath icon:(NSString *)iconPath {
    NSTask *task;
    task = [[NSTask alloc] init];
    [task setLaunchPath:@"/usr/bin/unzip"];
    NSArray *arguments = [NSArray arrayWithObjects: @"-o",appPath, iconPath, @"-d", @"/tmp", nil];
    [task setArguments: arguments];
    
    NSPipe *pipe;
    pipe = [NSPipe pipe];
    [task setStandardOutput: pipe];
    
    NSFileHandle *file;
    file = [pipe fileHandleForReading];
    
    [task launch];
    
    NSData *data;
    data = [file readDataToEndOfFile];
    
    NSString *string;
    string = [[NSString alloc] initWithData: data
                                   encoding: NSUTF8StringEncoding];
    
    //NSLog(@"aapt: %@", string);
    
    NSString *tmpIconPath = [[NSString alloc] initWithFormat:@"/tmp/%@", iconPath];
    
    [icon setImage:[[NSImage alloc] initByReferencingFile:tmpIconPath]];
}

- (void) startTask:(NSString *)apppath {
    
    NSBundle *mainBundle = [NSBundle mainBundle];
    NSString *path = [mainBundle pathForResource:@"adb" ofType:nil];
    
    AppInfo *appInfo = [self getApkInfo:apppath];
    
    NSTask *task = [[NSTask alloc] init];
    [task setLaunchPath:path];
    NSArray *arguments = [NSArray arrayWithObjects: @"install", apppath, nil];
    [task setArguments: arguments];
    
    NSPipe *pipe = [NSPipe pipe];
    [task setStandardOutput: pipe];
    //[task setStandardError: pipe];
    NSFileHandle *file = [pipe fileHandleForReading];
    
    instTask = task;

    [task launch];
    
    NSData *data = [file readDataToEndOfFile];
    NSString *string = [[NSString alloc] initWithData: data
                                   encoding: NSUTF8StringEncoding];
    NSLog (@"got\n%@", string);
    
    if([string rangeOfString:@"Failure"].location != NSNotFound) {
        if([string rangeOfString:@"INSTALL_FAILED_ALREADY_EXISTS"].location != NSNotFound) {
            [self taskDidTerminate:appInfo success:@"INSTALL_FAILED_ALREADY_EXISTS"];

            [text setStringValue:@"安装失败，应用已经存在"];
        }
    } else if([string rangeOfString:@"error: device not found"].location != NSNotFound) {
        NSLog(@"手机未连接");
        [text setStringValue:@"手机未连接，请插上手机后再重试"];
        [self taskDidTerminate:appInfo success:@"device not found"];
    } else {
        [self taskDidTerminate:appInfo success:nil];
    }
    
    /*
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(taskDidTerminate:)
                                                 name:NSTaskDidTerminateNotification
                                               object:nil];
     */
    
    NSLog(@"startTask end");
}


- (BOOL)readFromURL:(NSURL *)url ofType:(NSString *)typeName error:(NSError *__autoreleasing *)outError
{
    
    [self performSelectorInBackground:@selector(startTask:) withObject:[url path]];
    
    return YES;
}

- (void) taskDidTerminate:(AppInfo *)appInfo success:(NSString *)success {
    NSLog(@"end");
    
    if(success == nil) {
        [self performSelectorInBackground:@selector(updateUI:) withObject:appInfo];
    } else {
        NSArray *array = [NSArray arrayWithObjects:appInfo, success, nil];
        [self performSelectorInBackground:@selector(updateUIFailed:) withObject:array];
    }
}

- (void) updateUIFailed:(NSArray *)array {
    NSString *success = [array objectAtIndex:1];
    
    [progress stopAnimation:self];
    [progress setHidden:YES];
    
    //NSString *show =[[NSString alloc] initWithFormat:@"安装失败, %@", success];
    if([success isEqualToString:@"INSTALL_FAILED_ALREADY_EXISTS"]) {
        [cancelBtn setTitle:@"卸载后重装"];
    } else if([success isEqualToString:@"device not found"]) {
        [cancelBtn setTitle:@"再次安装"];
    }
    //[text setStringValue:show];
    
}

- (void) updateUI:(AppInfo *)appInfo {
    [progress stopAnimation:self];
    [progress setHidden:YES];
    
    [text setStringValue:@"安装完成"];
    [cancelBtn setTitle:@"打开"];
}

- (IBAction)cancel:(id)sender {

    if([[cancelBtn title] isEqualToString:@"打开"]) {
        NSString *lau = [[self curAppInfo] launch];
        NSString *pac = [[self curAppInfo] package];
        NSLog(@"clicked cancel, %@, %@", [[self curAppInfo] launch], pac);
        
        NSBundle *mainBundle=[NSBundle mainBundle];
        NSString *path=[mainBundle pathForResource:@"adb" ofType:nil];
        
        NSTask *task = [[NSTask alloc] init];
        [task setLaunchPath:path];
        
        NSString *cmp = [[NSString alloc] initWithFormat:@"%@/%@",pac, lau];
        NSLog(@"cmp: %@", cmp);
        NSArray *arguments = [NSArray arrayWithObjects:@"shell", @"am", @"start", @"-n", cmp, nil];
        [task setArguments: arguments];
        
        
        NSPipe *pipe = [NSPipe pipe];
        [task setStandardOutput: pipe];
        NSFileHandle *file = [pipe fileHandleForReading];
        
        [task launch];
        
        NSData *data = [file readDataToEndOfFile];
        NSString *string = [[NSString alloc] initWithData: data
                                                 encoding: NSUTF8StringEncoding];
        NSLog (@"got\n%@", string);
        
    } else if ([[cancelBtn title] isEqualToString:@"卸载后重装"]) {
        [progress startAnimation:self];
        [progress setHidden:NO];
        [text setStringValue:@"安装中"];
        
        NSString *pac = [[self curAppInfo] package];
        
        NSBundle *mainBundle=[NSBundle mainBundle];
        NSString *path=[mainBundle pathForResource:@"adb" ofType:nil];
        
        NSTask *task = [[NSTask alloc] init];
        [task setLaunchPath:path];
        
        NSLog(@"pac: %@", pac);
        
        NSArray *arguments = [NSArray arrayWithObjects:@"uninstall", pac, nil];
        [task setArguments: arguments];
        
        
        NSPipe *pipe = [NSPipe pipe];
        [task setStandardOutput: pipe];
        NSFileHandle *file = [pipe fileHandleForReading];
        
        [task launch];
        
        NSData *data = [file readDataToEndOfFile];
        NSString *string = [[NSString alloc] initWithData: data
                                                 encoding: NSUTF8StringEncoding];
        NSLog (@"got\n%@", string);
        
        [self performSelectorInBackground:@selector(startTask:) withObject:[[self fileURL] path]];
        
    } else if ([[cancelBtn title] isEqualToString:@"再次安装"]) {
        [self performSelectorInBackground:@selector(startTask:) withObject:[[self fileURL] path]];
    } else {
        [text setStringValue:@"取消安装"];
        [progress startAnimation:self];
        
        if(instTask != nil) {
            [instTask terminate];
        }
        
        [self close];
    }
    
}
@end
