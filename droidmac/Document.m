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
    // Insert code here to write your document to data of the specified type. If outError != NULL, ensure that you create and set an appropriate error when returning nil.
    // You can also choose to override -fileWrapperOfType:error:, -writeToURL:ofType:error:, or -writeToURL:ofType:forSaveOperation:originalContentsURL:error: instead.
    NSException *exception = [NSException exceptionWithName:@"UnimplementedMethod" reason:[NSString stringWithFormat:@"%@ is unimplemented", NSStringFromSelector(_cmd)] userInfo:nil];
    @throw exception;
    return nil;
}

- (BOOL)readFromData:(NSData *)data ofType:(NSString *)typeName error:(NSError **)outError
{
    // Insert code here to read your document from the given data of the specified type. If outError != NULL, ensure that you create and set an appropriate error when returning NO.
    // You can also choose to override -readFromFileWrapper:ofType:error: or -readFromURL:ofType:error: instead.
    // If you override either of these, you should also override -isEntireFileLoaded to return NO if the contents are lazily loaded.
    //    NSException *exception = [NSException exceptionWithName:@"UnimplementedMethod" reason:[NSString stringWithFormat:@"%@ is unimplemented", NSStringFromSelector(_cmd)] userInfo:nil];
    //    @throw exception;
    
    //self.fileURL;
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
    
    //NSLog(@"aapt: %@", string);
    
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
    
    
    
    NSBundle *mainBundle=[NSBundle mainBundle];
    NSString *path=[mainBundle pathForResource:@"adb" ofType:nil];
    
    AppInfo * appInfo = [self getApkInfo:apppath];
    
    
    NSTask *task;
    task = [[NSTask alloc] init];
    [task setLaunchPath:path];
    NSArray *arguments = [NSArray arrayWithObjects: @"install", apppath, nil];
    [task setArguments: arguments];
    
    
    NSPipe *pipe = [NSPipe pipe];
    [task setStandardOutput: pipe];
    NSFileHandle *file = [pipe fileHandleForReading];
    
    /*
    NSPipe *errPipe = [NSPipe pipe];
    [task setStandardError:errPipe];
    NSFileHandle *errFile = [errPipe fileHandleForReading];
     */
        
    [task launch];
    
    /*
    NSData *errData = [errFile availableData];
    NSString *errString = [[NSString alloc] initWithData: errData
                                             encoding: NSUTF8StringEncoding];
    NSLog (@"got err\n%@", errString);

     */
    
    NSData *data = [file readDataToEndOfFile];
    NSString *string = [[NSString alloc] initWithData: data
                                   encoding: NSUTF8StringEncoding];
    NSLog (@"got\n%@", string);
    
    /*
    if([string rangeOfString:@"error: device not found"].location != NSNotFound) {
        NSLog(@"手机未连接");
        [text setStringValue:@"手机未连接"];
    } else {
        NSLog(@"手机已链接");
    }
     */
    
    /*
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(taskDidTerminate:)
                                                 name:NSTaskDidTerminateNotification
                                               object:nil];
     */
    [self taskDidTerminate:appInfo];
    
    NSLog(@"ns end");
    
}


- (BOOL)readFromURL:(NSURL *)url ofType:(NSString *)typeName error:(NSError *__autoreleasing *)outError
{
    
    [self performSelectorInBackground:@selector(startTask:) withObject:[url path]];
    
    return YES;
}

- (void) taskDidTerminate:(AppInfo *)appInfo {
    NSLog(@"end");
    
    [self performSelectorInBackground:@selector(updateUI:) withObject:appInfo];
    
    
}

- (void) updateUI:(AppInfo *)appInfo {
    [progress stopAnimation:self];
    [progress setHidden:YES];
    
    NSLog(@"launch: %@", appInfo.launch);
    
    [text setStringValue:@"安装完成"];
    [cancelBtn setTitle:@"打开"];
    
    [self setCurAppInfo:appInfo];
    
    //[cancelBtn setStringValue:[appInfo launch]];
    
    /*
    [text setStringValue:@"安装成功"];
    [cancelBtn setStringValue:@"打开"];
     */
    
    // adb shell am start
    
    /*
    NSAlert *alert = [[NSAlert alloc] init];
    [alert setMessageText: [NSString stringWithFormat:@"read from"]];
    [alert setInformativeText:@"task finish"];
    [alert runModal];
     */
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
        
    } else {
        [text setStringValue:@"取消安装"];
        [progress startAnimation:self];
    }
   
    
    
}
@end
