// license:BSD-3-Clause
//============================================================
//
//  myosdmain.cpp - main file for my OSD
//
//  MAME4DROID by David Valdeita (Seleuco)
//
//============================================================

#include <functional>   // only for oslog callback

// standard includes
#include <unistd.h>

// MAME headers
#include "osdepend.h"
#include "emu.h"
#include "emuopts.h"
#include "main.h"
#include "fileio.h"
#include "gamedrv.h"
#include "drivenum.h"
#include "romload.h"
#include "screen.h"
#include "softlist_dev.h"
#include "strconv.h"
#include "corestr.h"

#include "uiinput.h"

#include "modules/lib/osdobj_common.h"

// OSD headers
#include "video.h"
#include "myosd.h"


#include <android/log.h>

#define MIN(a,b) ((a)<(b) ? (a) : (b))
#define MAX(a,b) ((a)<(b) ? (b) : (a))

//============================================================
// MYOSD globals
//============================================================
int myosd_display_width;
int myosd_display_height;
int myosd_display_width_osd;
int myosd_display_height_osd;
extern int myosd_fps;

//============================================================
//  OPTIONS
//============================================================

static const options_entry s_option_entries[] =
{
    //  { OPTION_INIPATH,       INI_PATH,   OPTION_STRING,     "path to ini files" },

    // MYOSD options
    { nullptr,              nullptr,    core_options::option_type::HEADER,      "MYOSD OPTIONS" },
    { OPTION_VIDEO,         "myosd",    core_options::option_type::STRING,      "video output method: none,myosd" },
    { OPTION_SOUND,         "myosd",      core_options::option_type::STRING,      "sound output method: none,myosd" },
    { OSDOPTION_NUMSCREENS "(1-1)",              "1",              core_options::option_type::INTEGER,   "Does nothing in MAME4droid" },
    { OSDOPTION_GL_GLSL,                         "0",              core_options::option_type::BOOLEAN,   "Does nothing in MAME4droid" },
    { OSDOPTION_FILTER ";glfilter;flt",          "1",              core_options::option_type::BOOLEAN,   "Does nothing in MAME4droid" },
    { OSDOPTION_PRESCALE "(1-1)",               "1",              core_options::option_type::INTEGER,   "Does nothing in MAME4droid" },
    { OSDOPTION_WINDOW ";w",                     "0",              core_options::option_type::BOOLEAN,   "Does nothing in MAME4droid" },
    { OPTION_KEEPASPECT ";ka",                           "1",         core_options::option_type::BOOLEAN,    "Does nothing in MAME4droid" },
    { OSDOPTION_MAXIMIZE ";max",                 "1",              core_options::option_type::BOOLEAN,   "Does nothing in MAME4droid" },
    { OSDOPTION_WAITVSYNC ";vs",                 "0",              core_options::option_type::BOOLEAN,   "Does nothing in MAME4droid" },
    { OSDOPTION_SYNCREFRESH ";srf",              "0",              core_options::option_type::BOOLEAN,   "Does nothing in MAME4droid" },

    { OPTION_PADDLE_DEVICE ";paddle",                    "none",  core_options::option_type::STRING,     "Does nothing in MAME4droid" },
    { OPTION_ADSTICK_DEVICE ";adstick",                  "none",  core_options::option_type::STRING,     "Does nothing in MAME4droid" },
    { OPTION_PEDAL_DEVICE ";pedal",                      "none",  core_options::option_type::STRING,     "Does nothing in MAME4droid" },
    { OPTION_DIAL_DEVICE ";dial",                        "none",  core_options::option_type::STRING,     "Does nothing in MAME4droid" },
    { OPTION_TRACKBALL_DEVICE ";trackball",              "none",  core_options::option_type::STRING,     "Does nothing in MAME4droid" },
    { OPTION_LIGHTGUN_DEVICE,                            "none",  core_options::option_type::STRING,     "Does nothing in MAME4droid" },
    { OPTION_POSITIONAL_DEVICE,                          "none",  core_options::option_type::STRING,     "Does nothing in MAME4droid" },
    { OPTION_MOUSE_DEVICE,                               "none",     core_options::option_type::STRING,     "Does nothing in MAME4droid" },

    { KEYBOARDINPUT_PROVIDER,                OSDOPTVAL_AUTO,   core_options::option_type::STRING,    "provider for keyboard input: " },
    { MOUSEINPUT_PROVIDER,                   OSDOPTVAL_AUTO,   core_options::option_type::STRING,    "provider for mouse input: " },
    { LIGHTGUNINPUT_PROVIDER,                OSDOPTVAL_AUTO,   core_options::option_type::STRING,    "provider for lightgun input: " },
    { JOYSTICKINPUT_PROVIDER,                OSDOPTVAL_AUTO,   core_options::option_type::STRING,    "provider for joystick input: " },

    { OPTION_HISCORE,       "0",        core_options::option_type::BOOLEAN,     "enable hiscore system" },
    { OPTION_BEAM,          "1.0",      core_options::option_type::FLOAT,       "set vector beam width maximum" },
    { OPTION_BENCH,         "0",        core_options::option_type::INTEGER,     "benchmark for the given number of emulated seconds" },
    { OPTION_NUMPROCESSORS, "auto",     core_options::option_type::STRING,      "number of processors; this overrides the number the system reports" },

    { nullptr }
};

//============================================================
//  myosd_main
//============================================================

my_osd_interface *osdInterface = nullptr;

extern "C" int myosd_main(int argc, char** argv, myosd_callbacks* callbacks, size_t callbacks_size)
{
    myosd_callbacks host_callbacks;
    memset(&host_callbacks, 0, sizeof(host_callbacks));
    memcpy(&host_callbacks, callbacks, MIN(sizeof(host_callbacks), sizeof(myosd_callbacks)));

    if (argc == 0) {
        static const char* args[] = {"myosd"};
        argc = 1;
        argv = (char**)args;
    }

    std::vector<std::string> args = osd_get_command_line(argc, argv);

    // tons of code in MAME does a unsafe downcast from emu_options to osd_options, be carefull
    // ...(we need to to have video, and sound in options entries)
    emu_options options;
    options.add_entries(s_option_entries);
    osdInterface = new my_osd_interface(options, host_callbacks);
    int res = emulator_info::start_frontend(options, *osdInterface, args);
    delete osdInterface;
    osdInterface = nullptr;
    return res;
}

extern "C" void myosd_pause(bool pause)
{
    if(osdInterface!= nullptr && osdInterface->isMachine() )
    {
        pause ? osdInterface->machine().pause() : osdInterface->machine().resume();

        /*
        auto machine = &osdInterface->machine();
        if(machine)
        {
            pause ? machine.pause() : machine.resume();
        }
         */
    }
}

extern "C" bool myosd_is_paused()
{
    if(osdInterface!= nullptr && osdInterface->isMachine())
    {
        return osdInterface->machine().paused();
    }
    return false;
}

//============================================================
//  myosd_pushEvent
//============================================================
extern "C" void myosd_pushEvent(myosd_inputevent event)
{
    if(osdInterface!= nullptr && osdInterface->isMachine() && osdInterface->target()!= nullptr) {

        switch(event.type) {
            case event.MYOSD_KEY_EVENT:
                osdInterface->machine().ui_input().push_char_event(osdInterface->target(), event.data.key_char);
                break;
            case event.MYOSD_MOUSE_MOVE_EVENT:
                osdInterface->machine().ui_input().push_mouse_move_event(osdInterface->target(), event.data.mouse_data.x, event.data.mouse_data.y);
                break;
            case event.MYOSD_MOUSE_BT1_DBLCLK:
                osdInterface->machine().ui_input().push_mouse_double_click_event(osdInterface->target(), event.data.mouse_data.x, event.data.mouse_data.y);
                break;
            case event.MYOSD_MOUSE_BT1_DOWN:
                osdInterface->machine().ui_input().push_mouse_down_event(osdInterface->target(), event.data.mouse_data.x, event.data.mouse_data.y);
                break;
            case event.MYOSD_MOUSE_BT1_UP:
                osdInterface->machine().ui_input().push_mouse_up_event(osdInterface->target(), event.data.mouse_data.x, event.data.mouse_data.y);
                break;
            case event.MYOSD_MOUSE_BT2_DOWN:
                osdInterface->machine().ui_input().push_mouse_rdown_event(osdInterface->target(), event.data.mouse_data.x, event.data.mouse_data.y);
                break;
            case event.MYOSD_MOUSE_BT2_UP:
                osdInterface->machine().ui_input().push_mouse_rup_event(osdInterface->target(), event.data.mouse_data.x, event.data.mouse_data.y);
                break;
            default:
                osd_printf_error("has unknown myosd event type (%u)\n",event.type);
        }

    }
}

//============================================================
//  myosd_get
//============================================================
extern "C" intptr_t myosd_get(int var)
{
    switch (var)
    {
        case MYOSD_MAME_VERSION:
            return (int)(atof(emulator_info::get_bare_build_version()) * 1000.0);

        case MYOSD_MAME_VERSION_STRING:
            return (intptr_t)(void*)emulator_info::get_build_version();

        case MYOSD_FPS:
            return 0;

        case MYOSD_SPEED:
            return 0;
    }
    return 0;
}

//============================================================
//  myosd_set
//============================================================
extern "C" void myosd_set(int var, intptr_t value)
{
    switch (var)
    {
        case MYOSD_DISPLAY_WIDTH:
            myosd_display_width = value;
            break;
        case MYOSD_DISPLAY_HEIGHT:
            myosd_display_height = value;
            break;
        case MYOSD_DISPLAY_WIDTH_OSD:
            myosd_display_width_osd = value;
            break;
        case MYOSD_DISPLAY_HEIGHT_OSD:
            myosd_display_height_osd = value;
            break;
        case MYOSD_FPS:
            myosd_fps = value;
            break;
        case MYOSD_SPEED:
            //myosd_speed = value;
            break;
        case MYOSD_VERSION:
            emulator_info::myosd_droid_version = (char*)(void*)value;
            break;
    }
}

//============================================================
//  constructor
//============================================================

my_osd_interface::my_osd_interface(emu_options &options, myosd_callbacks &callbacks)
: m_machine(nullptr),m_options(options), m_verbose(false), m_target(nullptr), m_callbacks(callbacks)
{
    osd_output::push(this);

    if (m_callbacks.output_init != NULL)
        m_callbacks.output_init();
}


//============================================================
//  destructor
//============================================================

my_osd_interface::~my_osd_interface()
{
    if (m_callbacks.output_exit != NULL)
        m_callbacks.output_exit();

    osd_output::pop(this);
}

//-------------------------------------------------
//  output_callback  - callback for osd_printf_...
//-------------------------------------------------

void my_osd_interface::output_callback(osd_output_channel channel, const util::format_argument_pack<char> &args)
{
    if (channel == OSD_OUTPUT_CHANNEL_VERBOSE && !m_verbose)
        return;

    std::ostringstream buffer;
    util::stream_format(buffer, args);

    if (m_callbacks.output_text != NULL)
    {
        _Static_assert((int)MYOSD_OUTPUT_ERROR == (int)OSD_OUTPUT_CHANNEL_ERROR);
        _Static_assert((int)MYOSD_OUTPUT_WARNING == (int)OSD_OUTPUT_CHANNEL_WARNING);
        _Static_assert((int)MYOSD_OUTPUT_INFO == (int)OSD_OUTPUT_CHANNEL_INFO);
        _Static_assert((int)MYOSD_OUTPUT_DEBUG == (int)OSD_OUTPUT_CHANNEL_DEBUG);
        _Static_assert((int)MYOSD_OUTPUT_VERBOSE == (int)OSD_OUTPUT_CHANNEL_VERBOSE);

        m_callbacks.output_text(channel, buffer.str().c_str());
    }
    else
    {
        static const char* channel_str[] = {"[ERROR]: ", "[WARN]: ", "[INFO]: ", "[DEBUG]: ", "[VERBOSE]: ", "[LOG]: "};
        fputs(channel_str[channel], stderr);
        fputs(buffer.str().c_str(), stderr);
    }
}

//============================================================
// get_game_info - convert game_driver to a myosd_game_info
//============================================================
static void get_game_info(myosd_game_info* info, const game_driver *driver, running_machine &machine)
{
    memset(info, 0, sizeof(myosd_game_info));

    /*
    device_type                 type;               // static type info for driver class
    const char *                parent;             // if this is a clone, the name of the parent
    const char *                year;               // year the game was released
    const char *                manufacturer;       // manufacturer of the game
    machine_creator_wrapper     machine_creator;    // machine driver tokens
    ioport_constructor          ipt;                // pointer to constructor for input ports
    driver_init_wrapper         driver_init;        // DRIVER_INIT callback
    const tiny_rom_entry *      rom;                // pointer to list of ROMs for the game
    const char *                compatible_with;
    const internal_layout *     default_layout;     // default internally defined layout
    machine_flags::type         flags;              // orientation and other flags; see defines above
    char                        name[MAX_DRIVER_NAME_CHARS + 1]; // short name of the game
    */

    //
    // MAME does not do device types anymore! (MAME 0.252+)
    //
    // what we are going to do is assume if a machine has no software list or media then it is an ARCADE
    // if it has keyboard input then it is a COMPUTER
    //
    info->type = MYOSD_GAME_TYPE_ARCADE;

    /*
    int type = (driver->flags & machine_flags::MASK_TYPE);

    if (type == MACHINE_TYPE_ARCADE)
        info->type = MYOSD_GAME_TYPE_ARCADE;
    else if (type == MACHINE_TYPE_CONSOLE)
        info->type = MYOSD_GAME_TYPE_CONSOLE;
    else if (type == MACHINE_TYPE_COMPUTER)
        info->type = MYOSD_GAME_TYPE_COMPUTER;
    else
        info->type = MYOSD_GAME_TYPE_OTHER;
    */

    info->source_file  = driver->type.source();
    info->parent       = driver->parent;
    info->name         = driver->name;
    info->description  = driver->type.fullname();
    info->year         = driver->year;
    info->manufacturer = driver->manufacturer;

    if (info->parent != NULL && info->parent[0] == '0' && info->parent[1] == 0)
        info->parent = "";

    if (driver->flags & (MACHINE_NOT_WORKING|MACHINE_UNEMULATED_PROTECTION))
        info->flags |= MYOSD_GAME_INFO_NOT_WORKING;

    if ((driver->flags & machine_flags::MASK_ORIENTATION) == machine_flags::ROT90 || (driver->flags & machine_flags::MASK_ORIENTATION) == machine_flags::ROT270)
        info->flags |= MYOSD_GAME_INFO_VERTICAL;

    if (driver->flags & MACHINE_IS_BIOS_ROOT)
        info->flags |= MYOSD_GAME_INFO_BIOS;

    if (driver->flags & (MACHINE_WRONG_COLORS | MACHINE_IMPERFECT_COLORS | MACHINE_IMPERFECT_GRAPHICS | MACHINE_NO_COCKTAIL))
        info->flags |= MYOSD_GAME_INFO_IMPERFECT_GRAPHICS;

    if (driver->flags & (MACHINE_NO_SOUND | MACHINE_IMPERFECT_SOUND | MACHINE_NO_SOUND_HW))
        info->flags |= MYOSD_GAME_INFO_IMPERFECT_SOUND;

    if (driver->flags & MACHINE_SUPPORTS_SAVE)
        info->flags |= MYOSD_GAME_INFO_SUPPORTS_SAVE;

    // check for a vector or LCD screen
    machine_config config(*driver, machine.options());
    for (const screen_device &device : screen_device_enumerator(config.root_device()))
    {
        if (device.screen_type() == SCREEN_TYPE_VECTOR)
            info->flags |= MYOSD_GAME_INFO_VECTOR;
        if (device.screen_type() == SCREEN_TYPE_LCD)
            info->flags |= MYOSD_GAME_INFO_LCD;
    }

    // get software lists for this system
    {
        static std::unordered_set<std::string> g_software;
        std::string software;

        software_list_device_enumerator swlistdev_iter(config.root_device());
        for (software_list_device &swlistdev : swlistdev_iter)
        {
            if (software.size() != 0)
                software.append(",");
            software.append(swlistdev.list_name());
        }

        // get all the file extensions for cart/flop/etc
        image_interface_enumerator img_iter(config.root_device());
        for (device_image_interface &img : img_iter)
        {
            // ignore things not user loadable
            if (!img.user_loadable())
                continue;

            osd_printf_debug("MEDIA: %s[%s]: '%s' (%s)%s\n", driver->name, img.brief_instance_name(), img.image_type_name(), img.file_extensions(), (img.must_be_loaded() ? "*" : ""));

            std::string media_type = img.brief_instance_name();

            // get the extensions and add them too as <type>:<extension> (ie cart:a26 or flop:t64)
            std::string extensions(img.file_extensions());
            for (int start = 0, end = extensions.find_first_of(',');; start = end + 1, end = extensions.find_first_of(',', start))
            {
                std::string curext(extensions, start, (end == -1) ? extensions.length() - start : end - start);

                if (curext.size() != 0) {
                    char ach[64];
                    snprintf(ach, sizeof(ach), "%s:%s", media_type.c_str(), curext.c_str());

                    if (software.size() != 0)
                        software.append(",");
                    software.append(ach);
                }

                if (end == -1)
                    break;
            }
        }

        if (software.size() != 0)
        {
            osd_printf_debug("SOFTWARE: '%s'\n", software.c_str());
            info->software_list = g_software.insert(software).first->c_str();
        }
    }

    if (info->software_list != NULL && info->software_list[0] != 0) {
        info->type = MYOSD_GAME_TYPE_CONSOLE;

        if (false)
            info->type = MYOSD_GAME_TYPE_COMPUTER;
    }
}

//============================================================
// get_romless_machines
// read (or create) romless.ini
//============================================================
static std::vector<std::string> get_romless_machines(running_machine &machine)
{
    std::vector<std::string> list;
    char line[256];
    char version[256];

    snprintf(version, sizeof(version), "; MAME %s\n", emulator_info::get_bare_build_version());
    FILE* file = fopen("romless.ini", "r");
    if (file == NULL || fgets(line, sizeof(line), file) == NULL || strcmp(line, version) != 0)
    {
        std::size_t const total = driver_list::total();

        fclose(file);
        file = fopen("romless.ini", "w");
        fputs(version, file);
        fputs("[romless machines]\n", file);

        osd_printf_debug("FIND ROMLESS MACHINES...\n");
        osd_ticks_t time = osd_ticks();

        // iterate over all machines and find romless machines
        for (int i = 0; i < total; i++)
        {
            game_driver const &driver(driver_list::driver(i));
            machine_config config(driver, machine.options());

            if (&driver == &GAME_NAME(___empty))
                continue;

            if (driver.flags & (MACHINE_NOT_WORKING | MACHINE_NO_SOUND | MACHINE_IS_INCOMPLETE | MACHINE_NO_SOUND_HW | MACHINE_MECHANICAL))
                continue;

            int num_roms = 0;
            for (device_t const &device : device_enumerator(config.root_device()))
            {
                for (const rom_entry *region = rom_first_region(device); region; region = rom_next_region(region))
                {
                    for (const rom_entry *rom = rom_first_file(region); rom; rom = rom_next_file(rom))
                    {
                        num_roms++;
                    }
                }
            }
            if (num_roms == 0)
            {
                fprintf(file, "%s\n", driver.name);
            }
        }

        time = osd_ticks() - time;
        osd_printf_debug("FIND ROMLESS MACHINES... took %0.3fsec\n", (float)time / osd_ticks_per_second());

        fclose(file);
        file = fopen("romless.ini", "r");
    }

    while (fgets(line, sizeof(line), file) != NULL)
    {
        if (line[strlen(line) - 1] == '\n') line[strlen(line) - 1] = '\0';
        if (line[strlen(line) - 1] == '\r') line[strlen(line) - 1] = '\0';

        if (line[0] == '\0' || line[0] == ';')
            continue;

        if (line[0] == '[')
            continue;

        list.push_back(line);
    }

    fclose(file);
    return list;
 }

//============================================================
// get_game_list - get list of available games
//============================================================
static std::vector<myosd_game_info> get_game_list(running_machine &machine)
{
    // this is the same code, and method, used by selgame.cpp
    std::size_t const total = driver_list::total();
    std::vector<bool> included(total, false);

    // iterate over ROM directories and look for potential ROMs
    file_enumerator path(machine.options().media_path());
    for (osd::directory::entry const *dir = path.next(); dir; dir = path.next())
    {
        char drivername[64];
        char *dst = drivername;
        char const *src;

        // build a name for it
        for (src = dir->name; *src != 0 && *src != '.' && dst < &drivername[std::size(drivername) - 1]; ++src)
            *dst++ = tolower(uint8_t(*src));

        *dst = 0;
        int const drivnum = driver_list::find(drivername);
        if (0 <= drivnum)
            included[drivnum] = true;
    }

    // add romless machines too.
    auto romless = get_romless_machines(machine);
    for(const auto& drivername: romless)
    {
        int const drivnum = driver_list::find(drivername.c_str());
        if (0 <= drivnum)
            included[drivnum] = true;
    }

    // now build a list of just avail games, as myosd_game_info(s)
    std::vector<myosd_game_info> list;
    for (int i = 0; i < total; i++)
    {
        game_driver const &driver(driver_list::driver(i));
        if (included[i]) {
            myosd_game_info info;
            get_game_info(&info, &driver, machine);
            list.push_back(info);
        }
    }
    return list;
}

//============================================================
//  init
//============================================================

void my_osd_interface::init(running_machine &machine)
{
    // This function is responsible for initializing the OSD-specific
    // video and input functionality, and registering that functionality
    // with the MAME core.
    //
    // In terms of video, this function is expected to create one or more
    // render_targets that will be used by the MAME core to provide graphics
    // data to the system. Although it is possible to do this later, the
    // assumption in the MAME core is that the user interface will be
    // visible starting at init() time, so you will have some work to
    // do to avoid these assumptions.
    //
    // In terms of input, this function is expected to enumerate all input
    // devices available and describe them to the MAME core by adding
    // input devices and their attached items (buttons/axes) via the input
    // system.
    //
    // Beyond these core responsibilities, init() should also initialize
    // any other OSD systems that require information about the current
    // running_machine.
    //
    // This callback is also the last opportunity to adjust the options
    // before they are consumed by the rest of the core.
    //
    m_machine = &machine;

    // ensure we get called on the way out
    machine.add_notifier(MACHINE_NOTIFY_EXIT, machine_notify_delegate(&my_osd_interface::machine_exit, this));

    auto &options = machine.options();

    // extract the verbose printing option
    if (options.verbose() || DebugLog > 1)
        set_verbose(true);

    // determine if we are benchmarking, and adjust options appropriately
    int bench = options.int_value(OPTION_BENCH);
    if (bench > 0)
    {
        options.set_value(OPTION_THROTTLE, false, OPTION_PRIORITY_MAXIMUM);
        options.set_value(OPTION_SOUND, "none", OPTION_PRIORITY_MAXIMUM);
        options.set_value(OPTION_VIDEO, "none", OPTION_PRIORITY_MAXIMUM);
        options.set_value(OPTION_SECONDS_TO_RUN, bench, OPTION_PRIORITY_MAXIMUM);
    }

    // check for HISCORE
    if (options.bool_value(OPTION_HISCORE))
    {
        // ...NOTE hiscores are handled via plugins
    }

    // check for OPTION_BEAM and map to OPTION_BEAM_WIDTH_MIN and MAX
    float beam = options.float_value(OPTION_BEAM);
    if (beam != 1.0)
    {
        options.set_value(OPTION_BEAM_WIDTH_MIN, beam, OPTION_PRIORITY_CMDLINE);
        options.set_value(OPTION_BEAM_WIDTH_MAX, beam, OPTION_PRIORITY_CMDLINE);
    }

    /* get number of processors */
    const char *nump = options.value(OPTION_NUMPROCESSORS);

    osd_num_processors = 0; // 0 is Auto

    if (strcmp(nump, "auto") != 0)
    {
        osd_num_processors = atoi(nump);
        if (osd_num_processors < 1)
        {
            osd_printf_warning("numprocessors < 1 doesn't make much sense. Assuming auto ...\n");
            osd_num_processors = 0;
        }
    }

    video_init();
    input_init();
    sound_init();

    bool in_game = (&m_machine->system() != &GAME_NAME(___empty));

    if (m_callbacks.game_init != NULL && in_game)
    {
        myosd_game_info info;
        get_game_info(&info, &machine.system(), machine);
        m_callbacks.game_init(&info);
    }
    else if (m_callbacks.game_list != NULL && !in_game)
    {
        std::vector<myosd_game_info> list = get_game_list(machine);
        m_callbacks.game_list(list.data(), list.size());
    }
}

//============================================================
//  machine_exit
//============================================================

void my_osd_interface::machine_exit()
{
    bool in_game = (&m_machine->system() != &GAME_NAME(___empty));

    if (m_callbacks.game_exit != NULL && in_game)
        m_callbacks.game_exit();

    video_exit();
    input_exit();
    sound_exit();
}

//============================================================
//  osd_setup_osd_specific_emu_options
//============================================================

void osd_setup_osd_specific_emu_options(emu_options &opts)
{
    opts.add_entries(s_option_entries);
}


